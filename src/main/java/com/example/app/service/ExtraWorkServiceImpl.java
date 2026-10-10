package com.example.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.BillingStatementItem;
import com.example.app.domain.BusinessPolicy;
import com.example.app.domain.ExtraWork;
import com.example.app.domain.ExtraWorkItem;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.dto.ExtraWorkCreateRequest;
import com.example.app.mapper.ExtraWorkMapper;
import com.example.app.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ExtraWorkServiceImpl implements ExtraWorkService {

	private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("10.00");
	private static final BigDecimal HUNDRED = new BigDecimal("100");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final DateTimeFormatter NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd");

	// 例外の案件（見積りを通さず、事前承認で受ける案件）の値
	private static final String CONTRACT_TYPE_TEMPORARY = "臨時";
	private static final String PROJECT_STATUS_ACTIVE = "進行中";
	private static final String PROJECT_STATUS_DONE = "完了";
	private static final int PROJECT_NAME_MAX = 30;
	private static final int STAFF_NAME_MAX = 30;

	private final ExtraWorkMapper extraWorkMapper;
	private final UserMapper userMapper;
	private final ProjectService projectService;
	private final ClientService clientService;
	private final CompanyService companyService;
	private final BusinessPolicyService businessPolicyService;
	private final EmailService emailService;

	// ①全体の一覧
	@Override
	public List<ExtraWork> searchExtraWorks(Integer clientId, Integer companyId, boolean openOnly) {
		return extraWorkMapper.search(clientId, companyId, openOnly);
	}

	// ②案件の作業
	@Override
	public ExtraWork getByProjectId(Integer projectId) {
		List<ExtraWork> works = extraWorkMapper.selectByProjectId(projectId);
		if (works.isEmpty()) {
			return null;
		}
		ExtraWork w = works.get(0);
		w.setItems(extraWorkMapper.selectItemsByExtraWorkId(w.getExtraWorkId()));
		return w;
	}

	// ③1件
	@Override
	public ExtraWork getExtraWork(Integer projectId, Integer extraWorkId) {
		ExtraWork w = extraWorkMapper.selectById(extraWorkId);
		if (w == null || !Objects.equals(w.getProjectId(), projectId)) {
			return null;
		}
		w.setItems(extraWorkMapper.selectItemsByExtraWorkId(extraWorkId));
		return w;
	}

	// ④新規受注：事前承認を先に確認し、だめなら案件も作らない（例外はロールバック）
	@Override
	public ExtraWork createWithProject(ExtraWorkCreateRequest request, String adminName) {
		if (request.getClientId() == null || clientService.getClientById(request.getClientId()) == null) {
			throw new IllegalArgumentException("顧客を選択してください。");
		}
		if (request.getCompanyId() == null || companyService.getCompanyById(request.getCompanyId()) == null) {
			throw new IllegalArgumentException("発注元を選択してください。");
		}
		ExtraWork form = request.getWork();
		if (form == null) {
			throw new IllegalArgumentException("作業の内容を入力してください。");
		}
		if (form.getTaxRate() == null) {
			form.setTaxRate(DEFAULT_TAX_RATE);
		}
		validate(form);
		BigDecimal rate = agreedMarkupRate(request.getCompanyId(), form.getWorkDate());

		Project project = new Project();
		project.setClientId(request.getClientId());
		project.setCompanyId(request.getCompanyId());
		project.setProjectName(projectNameOf(request.getProjectName(), form));
		project.setContractType(CONTRACT_TYPE_TEMPORARY);
		project.setOrderRoute(Project.ORDER_ROUTE_POLICY);
		project.setStatus(PROJECT_STATUS_ACTIVE);
		project.setProjectStaffname(limit(adminName, STAFF_NAME_MAX));
		projectService.addProject(project);

		form.setExtraWorkId(null);
		form.setProjectId(project.getProjectId());
		form.setMarkupRate(rate);
		form.setStatus(ExtraWork.STATUS_DRAFT);
		extraWorkMapper.insert(form);
		insertItems(form.getExtraWorkId(), form.getItems());
		return form;
	}

	// ⑤保存：行はいったん削除して入れ直す（毎次明細へは値をコピーするため、ほかから参照されない）
	@Override
	public boolean save(Integer projectId, Integer extraWorkId, ExtraWork form) {
		Project project = getProject(projectId);
		ExtraWork current = getExisting(projectId, extraWorkId);
		if (isBilled(current)) {
			throw new IllegalStateException(billedMessage(current) + "［請求済みを取り消す］をしてから編集してください。");
		}
		if (form.getTaxRate() == null) {
			form.setTaxRate(current.getTaxRate());
		}
		validate(form);
		form.setExtraWorkId(extraWorkId);
		form.setProjectId(projectId);
		form.setMarkupRate(agreedMarkupRate(project.getCompanyId(), form.getWorkDate()));
		extraWorkMapper.update(form);
		extraWorkMapper.deleteItemsByExtraWorkId(extraWorkId);
		insertItems(extraWorkId, form.getItems());

		// 受注済みの内容を変えたら、受注し直してもらうため保留に戻す
		if (ExtraWork.STATUS_ORDERED.equals(current.getStatus())) {
			changeStatus(extraWorkId, ExtraWork.STATUS_PENDING, null, null);
			return true;
		}
		return false;
	}

	// ⑥受注
	@Override
	public int order(Integer projectId, Integer extraWorkId, String orderedBy) {
		Project project = getProject(projectId);
		ExtraWork work = getExisting(projectId, extraWorkId);
		if (ExtraWork.STATUS_ORDERED.equals(work.getStatus())) {
			throw new IllegalStateException("この作業はすでに受注済みです。");
		}
		// 保存してから事前承認が変わっていないか（実施日が先の日付のときに起こりうる）
		BigDecimal rate = agreedMarkupRate(project.getCompanyId(), work.getWorkDate());
		if (rate.compareTo(work.getMarkupRate()) != 0) {
			throw new IllegalStateException("実施日の時点の加算割合が、保存したとき（" + work.getMarkupRate()
					+ "%）から " + rate + "% に変わっています。編集画面で保存し直してから受注してください。");
		}

		LocalDateTime now = LocalDateTime.now();
		changeStatus(extraWorkId, ExtraWork.STATUS_ORDERED, now, orderedBy);
		work.setOrderedAt(now);
		work.setOrderedBy(orderedBy);
		return notifyOrdered(project, work);
	}

	// ⑦保留
	@Override
	public void hold(Integer projectId, Integer extraWorkId) {
		ExtraWork work = getExisting(projectId, extraWorkId);
		if (!ExtraWork.STATUS_DRAFT.equals(work.getStatus())) {
			throw new IllegalStateException("保留にできるのは下書きの作業だけです。");
		}
		changeStatus(extraWorkId, ExtraWork.STATUS_PENDING, null, null);
	}

	// ⑧下書きに戻す
	@Override
	public void revertToDraft(Integer projectId, Integer extraWorkId) {
		ExtraWork work = getExisting(projectId, extraWorkId);
		if (!ExtraWork.STATUS_ORDERED.equals(work.getStatus())) {
			throw new IllegalStateException("下書きに戻せるのは受注済みの作業だけです。");
		}
		if (isImported(work)) {
			throw new IllegalStateException(importedMessage(work) + "毎次明細から行を削除してから、下書きに戻してください。");
		}
		if (isBilled(work)) {
			throw new IllegalStateException(billedMessage(work) + "［請求済みを取り消す］をしてから、下書きに戻してください。");
		}
		changeStatus(extraWorkId, ExtraWork.STATUS_DRAFT, null, null);
	}

	// ⑨削除
	@Override
	public boolean delete(Integer projectId, Integer extraWorkId) {
		Project project = getProject(projectId);
		ExtraWork work = getExisting(projectId, extraWorkId);
		if (isImported(work)) {
			throw new IllegalStateException(importedMessage(work) + "毎次明細から行を削除してから、削除してください。");
		}
		if (isBilled(work)) {
			throw new IllegalStateException(billedMessage(work) + "［請求済みを取り消す］をしてから、削除してください。");
		}
		if (ExtraWork.STATUS_ORDERED.equals(work.getStatus())) {
			throw new IllegalStateException("受注済みの作業は削除できません。下書きに戻してから削除してください。");
		}
		extraWorkMapper.deleteItemsByExtraWorkId(extraWorkId);
		extraWorkMapper.delete(extraWorkId);

		// 例外の案件は作業のための案件なので、一緒に削除する（論理削除。削除済み一覧から復元できる）
		if (Project.ORDER_ROUTE_POLICY.equals(project.getOrderRoute())) {
			projectService.deleteProject(projectId);
			return true;
		}
		return false;
	}

	// ⑩請求済みにする
	@Override
	public void markBilled(Integer projectId, Integer extraWorkId, LocalDate billedDate) {
		ExtraWork work = getExisting(projectId, extraWorkId);
		if (!ExtraWork.STATUS_ORDERED.equals(work.getStatus())) {
			throw new IllegalStateException("請求済みにできるのは受注済みの作業だけです。");
		}
		if (isImported(work)) {
			throw new IllegalStateException(importedMessage(work) + "毎次明細で請求するため、請求済みにはできません。");
		}
		if (isBilled(work)) {
			throw new IllegalStateException("この作業はすでに請求済みです。");
		}
		if (billedDate == null) {
			throw new IllegalArgumentException("請求日を入力してください。");
		}
		extraWorkMapper.updateBilledDate(extraWorkId, billedDate);
		projectService.updateStatus(projectId, PROJECT_STATUS_DONE);
	}

	// ⑪請求済みを取り消す
	@Override
	public void unmarkBilled(Integer projectId, Integer extraWorkId) {
		ExtraWork work = getExisting(projectId, extraWorkId);
		if (!isBilled(work)) {
			throw new IllegalStateException("この作業は請求済みではありません。");
		}
		extraWorkMapper.updateBilledDate(extraWorkId, null);
		projectService.updateStatus(projectId, PROJECT_STATUS_ACTIVE);
	}

	// ⑫毎次明細に取り込める作業
	@Override
	public List<ExtraWork> getImportCandidates(Integer clientId, Integer companyId) {
		List<ExtraWork> works = extraWorkMapper.search(clientId, companyId, true).stream()
				.filter(w -> ExtraWork.STATUS_ORDERED.equals(w.getStatus()))
				.toList();
		works.forEach(w -> w.setItems(extraWorkMapper.selectItemsByExtraWorkId(w.getExtraWorkId())));
		return works;
	}

	// ⑬1件（案件を問わない）
	@Override
	public ExtraWork getById(Integer extraWorkId) {
		return extraWorkMapper.selectById(extraWorkId);
	}

	// ⑭例外の案件の状態を、請求・取り込みに合わせる
	@Override
	public void syncProjectStatus(Integer extraWorkId) {
		ExtraWork work = extraWorkMapper.selectById(extraWorkId);
		if (work == null) {
			return;
		}
		boolean done = isBilled(work) || isImported(work);
		projectService.updateStatus(work.getProjectId(), done ? PROJECT_STATUS_DONE : PROJECT_STATUS_ACTIVE);
	}


	// ---------- 共通 ----------

	private Project getProject(Integer projectId) {
		Project project = projectService.getProjectById(projectId); // 削除済みの案件は null
		if (project == null) {
			throw new IllegalArgumentException("案件が見つかりません。");
		}
		return project;
	}

	private ExtraWork getExisting(Integer projectId, Integer extraWorkId) {
		ExtraWork work = getExtraWork(projectId, extraWorkId);
		if (work == null) {
			throw new IllegalArgumentException("緊急・追加作業が見つかりません。");
		}
		return work;
	}

	// 案件名：入力があればそれ（30文字まで）。なければ「MM/dd 依頼主 依頼内容」を30文字で切る
	// （依頼主・依頼内容は validate で必須・前後の空白を取り除き済み）
	private String projectNameOf(String input, ExtraWork form) {
		String name = trim(input);
		if (name != null) {
			if (name.length() > PROJECT_NAME_MAX) {
				throw new IllegalArgumentException("案件名は" + PROJECT_NAME_MAX + "文字以内で入力してください。");
			}
			return name;
		}
		return limit(form.getWorkDate().format(NAME_DATE_FORMAT) + " " + form.getRequester() + " " + form.getContent(),
				PROJECT_NAME_MAX);
	}



	// 実施日の時点で事前承認が「承認する」なら加算割合を返す（でなければ登録・受注できない）
	private BigDecimal agreedMarkupRate(Integer companyId, LocalDate workDate) {
		BusinessPolicy policy = businessPolicyService.getPolicyOn(companyId, workDate);
		if (policy == null || !Objects.equals(policy.getIsAgreed(), 1) || policy.getMarkupRate() == null) {
			throw new IllegalStateException("実施日（" + workDate.format(DATE_FORMAT)
					+ "）の時点で発注元の事前承認が「承認する」になっていないため、緊急・追加作業を登録・受注できません。");
		}
		return policy.getMarkupRate();
	}

	private void validate(ExtraWork form) {
		String type = form.getWorkType();
		if (!BillingStatementItem.WORK_TYPE_EMERGENCY.equals(type)
				&& !BillingStatementItem.WORK_TYPE_ADDITIONAL.equals(type)) {
			throw new IllegalArgumentException("区分を選択してください。");
		}
		if (form.getWorkDate() == null) {
			throw new IllegalArgumentException("実施日を入力してください。");
		}
		form.setLocation(trim(form.getLocation()));
		form.setContent(trim(form.getContent()));
		form.setRequester(trim(form.getRequester()));
		if (form.getRequester() == null || form.getRequester().length() > 50) {
			throw new IllegalArgumentException("依頼主を入力してください（50文字以内）。");
		}
		if (form.getLocation() != null && form.getLocation().length() > 100) {
			throw new IllegalArgumentException("場所は100文字以内で入力してください。");
		}
		if (form.getContent() == null || form.getContent().length() > 255) {
			throw new IllegalArgumentException("依頼内容を入力してください（255文字以内）。");
		}
		if (form.getTaxRate().signum() < 0 || form.getTaxRate().compareTo(HUNDRED) >= 0) {
			throw new IllegalArgumentException("税率は0〜99.99の範囲で入力してください。");
		}

		List<ExtraWorkItem> items = form.getItems();
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("作業の行を1行以上入力してください。");
		}
		for (int i = 0; i < items.size(); i++) {
			ExtraWorkItem item = items.get(i);
			String label = (i + 1) + "行目：";
			item.setItemName(trim(item.getItemName()));
			item.setUnit(trim(item.getUnit()));
			if (item.getItemName() == null || item.getItemName().length() > 100) {
				throw new IllegalArgumentException(label + "項目は1〜100文字で入力してください。");
			}
			if (item.getQuantity() == null || item.getQuantity() <= 0) {
				throw new IllegalArgumentException(label + "数量は1以上で入力してください。");
			}
			if (item.getUnit() == null || item.getUnit().length() > 20) {
				throw new IllegalArgumentException(label + "単位は1〜20文字で入力してください。");
			}
			if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
				throw new IllegalArgumentException(label + "単価を0以上で入力してください。");
			}
		}
	}

	private void insertItems(Integer extraWorkId, List<ExtraWorkItem> items) {
		int order = 1;
		for (ExtraWorkItem item : items) {
			item.setExtraWorkItemId(null);
			item.setExtraWorkId(extraWorkId);
			item.setDisplayOrder(order++);
			extraWorkMapper.insertItem(item);
		}
	}

	private void changeStatus(Integer extraWorkId, String status, LocalDateTime orderedAt, String orderedBy) {
		ExtraWork w = new ExtraWork();
		w.setExtraWorkId(extraWorkId);
		w.setStatus(status);
		w.setOrderedAt(orderedAt);
		w.setOrderedBy(orderedBy);
		extraWorkMapper.updateStatus(w);
	}

	private boolean isImported(ExtraWork work) {
		return work.getImportedStatementId() != null;
	}

	private boolean isBilled(ExtraWork work) {
		return work.getBilledDate() != null;
	}

	private String importedMessage(ExtraWork work) {
		return "この作業は毎次明細（" + work.getImportedBillingMonth() + "）に取り込み済みです。";
	}

	private String billedMessage(ExtraWork work) {
		return "この作業は請求済み（" + work.getBilledDate().format(DATE_FORMAT) + "）です。";
	}

	// 業者の代表ユーザー（有効な人）にお知らせメールを送る。送った人数を返す
	private int notifyOrdered(Project project, ExtraWork work) {
		List<User> masters = userMapper.selectByCompanyId(project.getCompanyId()).stream()
				.filter(u -> Objects.equals(u.getRoleFlag(), 2) && Objects.equals(u.getIsActive(), 1))
				.toList();
		BigDecimal subtotal = presentedSubtotal(work);
		BigDecimal tax = subtotal.multiply(work.getTaxRate()).divide(HUNDRED, 0, RoundingMode.DOWN); // 税は切り捨て
		for (User u : masters) {
			emailService.sendExtraWorkOrderedNotification(u.getEmail(), project, work, subtotal, tax);
		}
		return masters.size();
	}

	// 業者→顧客（税別）：行ごとに 当社→業者 ×（1＋加算割合）を四捨五入して合計（画面の presentedAmount と同じ）
	private BigDecimal presentedSubtotal(ExtraWork work) {
		BigDecimal factor = BigDecimal.ONE.add(work.getMarkupRate().divide(HUNDRED));
		return work.getItems().stream()
				.map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
						.multiply(factor).setScale(0, RoundingMode.HALF_UP))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private String trim(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	private String limit(String s, int max) {
		return (s == null || s.length() <= max) ? s : s.substring(0, max);
	}
}
