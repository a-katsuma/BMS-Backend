package com.example.app.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingBaseItem;
import com.example.app.domain.BillingBaseOtherItem;
import com.example.app.domain.BillingBaseVersion;
import com.example.app.domain.BillingStatement;
import com.example.app.domain.BillingStatementItem;
import com.example.app.domain.BillingStatementOtherItem;
import com.example.app.domain.BusinessPolicy;
import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;
import com.example.app.mapper.BillingStatementMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BillingStatementServiceImpl implements BillingStatementService {

	private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

	private final BillingStatementMapper statementMapper;
	private final BillingBaseService billingBaseService;
	private final ProjectService projectService;
	private final BusinessPolicyService businessPolicyService;
	private final ExtraWorkService extraWorkService;

	// ①一覧
	@Override
	public List<BillingStatement> getStatements(Integer projectId, boolean confirmedOnly) {
		return statementMapper.selectByProjectId(projectId, confirmedOnly);
	}

	// ②1件
	@Override
	public BillingStatement getStatement(Integer projectId, Integer statementId) {
		BillingStatement s = statementMapper.selectById(statementId);
		if (s == null || !Objects.equals(s.getProjectId(), projectId)) {
			return null;
		}
		List<BillingStatementItem> all = statementMapper.selectItemsByStatementId(statementId);
		s.setItems(all.stream().filter(i -> !isTemporary(i)).toList());
		s.setTemporaryItems(all.stream().filter(this::isTemporary).toList());
		s.setOtherItems(statementMapper.selectOtherItemsByStatementId(statementId));
		return s;
	}

	// ③作成：指定したベースの現在の版の値をコピーして持つ（使用中のベースが1つだけなら省略可）
	@Override
	public Integer create(Integer projectId, Integer baseId, String billingMonth, String createdBy) {
		if (billingMonth == null || !MONTH_PATTERN.matcher(billingMonth).matches()) {
			throw new IllegalArgumentException("請求年月を正しく入力してください。");
		}
		if (baseId == null) {
			List<BillingBase> activeBases = billingBaseService.getBases(projectId, true);
			if (activeBases.isEmpty()) {
				throw new IllegalStateException("使用中のベース明細がないため、毎次明細を作成できません。");
			}
			if (activeBases.size() > 1) {
				throw new IllegalArgumentException("コピー元のベースを選択してください。");
			}
			baseId = activeBases.get(0).getBaseId();
		}
		BillingBase base = billingBaseService.getBase(projectId, baseId);
		if (base == null) {
			throw new IllegalArgumentException("指定されたベースはこの案件のものではありません。");
		}
		if (base.isStopped()) {
			throw new IllegalStateException("使用停止中のベースからは毎次明細を作成できません。");
		}
		if (base.getCurrentVersion() == null) {
			throw new IllegalStateException("ベース明細の版がないため、毎次明細を作成できません。");
		}
		BillingBaseVersion version = base.getCurrentVersion();



		BillingStatement statement = new BillingStatement();
		statement.setBaseId(base.getBaseId());
		statement.setVersionId(version.getVersionId());
		statement.setBillingMonth(billingMonth);
		statement.setTaxRate(version.getTaxRate());
		statement.setStatus(BillingStatement.STATUS_DRAFT);
		statement.setCreatedBy(createdBy);
		statementMapper.insert(statement);

		int order = 1;
		for (BillingBaseItem b : version.getItems()) {
			BillingStatementItem item = new BillingStatementItem();
			item.setStatementId(statement.getStatementId());
			item.setBuilding(b.getBuilding());
			item.setItemName(b.getItemName());
			item.setUnit(b.getUnit());
			item.setBaseQuantity(b.getBaseQuantity());
			item.setExcludedQuantity(b.getExcludedQuantity() == null ? 0 : b.getExcludedQuantity());
			item.setExcludedReason(b.getExcludedReason());
			item.setAdjustmentQuantity(0);
			item.setUnitPrice(b.getUnitPrice());
			item.setDisplayOrder(order++);
			statementMapper.insertItem(item);
		}

		order = 1;
		for (BillingBaseOtherItem b : version.getOtherItems()) {
			boolean isVariable = b.getIsVariable() != null && b.getIsVariable() == 1;
			BillingStatementOtherItem other = new BillingStatementOtherItem();
			other.setStatementId(statement.getStatementId());
			other.setFeeName(b.getFeeName());
			other.setQuantity(b.getDefaultQuantity());
			other.setUnitPrice(isVariable ? BigDecimal.ZERO : b.getDefaultUnitPrice()); // 実費は明細ごとに入力
			other.setIsVariable(isVariable ? 1 : 0);
			other.setDisplayOrder(order++);
			statementMapper.insertOtherItem(other);
		}
		return statement.getStatementId();
	}

	// ④下書きの保存
	@Override
	public void save(Integer projectId, Integer statementId, BillingStatement form) {
		BillingStatement current = getDraft(projectId, statementId);

		Map<Integer, BillingStatementItem> itemMap = current.getItems().stream()
				.collect(Collectors.toMap(BillingStatementItem::getStatementItemId, Function.identity()));
		for (BillingStatementItem input : nullToEmpty(form.getItems())) {
			BillingStatementItem saved = itemMap.get(input.getStatementItemId());
			if (saved == null) {
				throw new IllegalArgumentException("明細行が見つかりません。画面を再読み込みしてください。");
			}
			String label = saved.getBuilding() + " " + saved.getItemName() + "：";
			int adjustment = (input.getAdjustmentQuantity() == null) ? 0 : input.getAdjustmentQuantity();
			int max = saved.getBaseQuantity() - saved.getExcludedQuantity();
			if (adjustment > 0 || adjustment < -max) {
				throw new IllegalArgumentException(label + "調整数は-" + max + "〜0の範囲で、マイナスで入力してください。");
			}

			saved.setAdjustmentQuantity(adjustment);
			saved.setAdjustmentReason(adjustment == 0 ? null : limit(trim(input.getAdjustmentReason()), 255));
			statementMapper.updateItemAdjustment(saved);
		}

		Map<Integer, BillingStatementOtherItem> otherMap = current.getOtherItems().stream()
				.collect(Collectors.toMap(BillingStatementOtherItem::getStatementOtherItemId, Function.identity()));
		for (BillingStatementOtherItem input : nullToEmpty(form.getOtherItems())) {
			BillingStatementOtherItem saved = otherMap.get(input.getStatementOtherItemId());
			if (saved == null) {
				throw new IllegalArgumentException("その他項目が見つかりません。画面を再読み込みしてください。");
			}
			if (saved.getIsVariable() == null || saved.getIsVariable() != 1) {
				continue; // 固定の項目はベースの値のまま
			}
			String label = saved.getFeeName() + "：";
			if (input.getUnitPrice() == null || input.getUnitPrice().signum() < 0) {
				throw new IllegalArgumentException(label + "実費の単価を0以上で入力してください。");
			}
			if (input.getQuantity() == null || input.getQuantity() < 0) {
				throw new IllegalArgumentException(label + "数量を0以上で入力してください。");
			}
			saved.setUnitPrice(input.getUnitPrice());
			saved.setQuantity(input.getQuantity());
			statementMapper.updateOtherItem(saved);
		}

		// 緊急・追加作業：いったん削除して入れ直す（取り込み元は extra_work_id で持ち直す）
		saveTemporaryItems(projectId, statementId, current.getItems().size(), form.getTemporaryItems(),
				extraWorkIdsOf(current.getTemporaryItems()));

		statementMapper.touch(statementId);
	}

	// ⑤確定
	@Override
	public void confirm(Integer projectId, Integer statementId, BillingStatement form, String confirmedBy) {
		if (form.getIssuedDate() == null) {
			throw new IllegalArgumentException("発行日を入力してください。");
		}
		save(projectId, statementId, form);

		BillingStatement s = new BillingStatement();
		s.setStatementId(statementId);
		s.setStatus(BillingStatement.STATUS_CONFIRMED);
		s.setIssuedDate(form.getIssuedDate());
		s.setConfirmedBy(confirmedBy);
		s.setConfirmedAt(LocalDateTime.now());
		statementMapper.updateStatus(s);
	}

	// ⑥確定解除
	@Override
	public void unconfirm(Integer projectId, Integer statementId) {
		BillingStatement current = getStatement(projectId, statementId);
		if (current == null) {
			throw new IllegalArgumentException("毎次明細が見つかりません。");
		}
		if (!BillingStatement.STATUS_CONFIRMED.equals(current.getStatus())) {
			throw new IllegalStateException("この明細は確定されていません。");
		}
		BillingStatement s = new BillingStatement();
		s.setStatementId(statementId);
		s.setStatus(BillingStatement.STATUS_DRAFT);
		s.setIssuedDate(current.getIssuedDate()); // 再確定時の初期値として残す
		s.setConfirmedBy(null);
		s.setConfirmedAt(null);
		statementMapper.updateStatus(s);
	}

	// ⑦削除（下書きのみ）
	@Override
	public void delete(Integer projectId, Integer statementId) {
		BillingStatement current = getDraft(projectId, statementId);
		Set<Integer> imported = extraWorkIdsOf(current.getTemporaryItems());
		statementMapper.deleteItemsByStatementId(statementId);
		statementMapper.deleteOtherItemsByStatementId(statementId);
		statementMapper.delete(statementId);

		// 取り込みが外れた緊急・追加作業の案件を、進行中に戻す
		imported.forEach(extraWorkService::syncProjectStatus);
	}

	// ---------- 共通 ----------

	private BillingStatement getDraft(Integer projectId, Integer statementId) {
		BillingStatement s = getStatement(projectId, statementId);
		if (s == null) {
			throw new IllegalArgumentException("毎次明細が見つかりません。");
		}
		if (!BillingStatement.STATUS_DRAFT.equals(s.getStatus())) {
			throw new IllegalStateException("確定済みの明細は変更できません。確定解除してから編集してください。");
		}
		return s;
	}

	private <T> List<T> nullToEmpty(List<T> list) {
		return (list == null) ? List.of() : list;
	}

	private String trim(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	private String limit(String s, int max) {
		return (s == null || s.length() <= max) ? s : s.substring(0, max);
	}

	// 行の中の取り込み元の作業（重複なし）
	private Set<Integer> extraWorkIdsOf(List<BillingStatementItem> rows) {
		return nullToEmpty(rows).stream()
				.map(BillingStatementItem::getExtraWorkId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	// 緊急・追加作業の保存
	//   手入力の行：行ごとの実施日の時点で有効だった事前承認を記録する
	//   受注済みから取り込んだ行（extraWorkId あり）：作業に記録した加算割合を使う
	//     今回新しく取り込む作業は、同じ顧客・業者で、受注済み・未請求・ほかの明細に未取り込みかを確かめる
	//   最後に、取り込んだ・外した作業の案件の状態を合わせる（取り込み → 完了、外す → 進行中）
	private void saveTemporaryItems(Integer projectId, Integer statementId, int baseRowCount,
			List<BillingStatementItem> rows, Set<Integer> importedBefore) {
		statementMapper.deleteTemporaryItems(statementId);

		Project project = projectService.getProjectById(projectId);
		Set<Integer> candidateIds = null; // 新しく取り込む作業があるときだけ取る
		Map<Integer, ExtraWork> works = new HashMap<>();

		List<BillingStatementItem> list = nullToEmpty(rows);
		for (int i = 0; i < list.size(); i++) {
			BillingStatementItem row = list.get(i);
			String label = "緊急・追加作業 " + (i + 1) + "行目：";
			validateTemporary(row, i + 1);

			BigDecimal rate;
			Integer extraWorkId = row.getExtraWorkId();
			if (extraWorkId != null) {
				if (!importedBefore.contains(extraWorkId)) {
					if (candidateIds == null) {
						candidateIds = extraWorkService
								.getImportCandidates(project.getClientId(), project.getCompanyId()).stream()
								.map(ExtraWork::getExtraWorkId)
								.collect(Collectors.toSet());
					}
					if (!candidateIds.contains(extraWorkId)) {
						throw new IllegalStateException(label + "取り込めない作業です（受注済みでない・請求済み・"
								+ "ほかの明細に取り込み済み・別の顧客や発注元の作業のいずれかです）。画面を再読み込みしてください。");
					}
				}
				ExtraWork work = works.computeIfAbsent(extraWorkId, extraWorkService::getById);
				if (work == null) {
					throw new IllegalStateException(label + "取り込み元の作業が見つかりません。画面を再読み込みしてください。");
				}
				rate = work.getMarkupRate();
			} else {
				BusinessPolicy policy = businessPolicyService.getPolicyOn(project.getCompanyId(), row.getWorkDate());
				rate = (policy != null && Objects.equals(policy.getIsAgreed(), 1)) ? policy.getMarkupRate() : null;
			}

			row.setStatementItemId(null);
			row.setStatementId(statementId);
			row.setIsTemporary(1);
			row.setExcludedQuantity(0);
			row.setExcludedReason(null);
			row.setAdjustmentQuantity(0);
			row.setAdjustmentReason(null);
			row.setPolicyAgreed(rate != null ? 1 : 0);
			row.setMarkupRate(rate);
			row.setDisplayOrder(baseRowCount + i + 1);
			statementMapper.insertItem(row);
		}

		// 取り込みが変わった作業（前からあった＋今回の行にある）の案件の状態を合わせる
		Set<Integer> affected = new HashSet<>(importedBefore);
		affected.addAll(works.keySet());
		affected.forEach(extraWorkService::syncProjectStatus);
	}

	private void validateTemporary(BillingStatementItem row, int no) {
		String label = "緊急・追加作業 " + no + "行目：";
		if (!BillingStatementItem.WORK_TYPE_EMERGENCY.equals(row.getWorkType())
				&& !BillingStatementItem.WORK_TYPE_ADDITIONAL.equals(row.getWorkType())) {
			throw new IllegalArgumentException(label + "区分を選択してください。");
		}
		if (row.getWorkDate() == null) {
			throw new IllegalArgumentException(label + "実施日を入力してください。");
		}
		row.setBuilding(trim(row.getBuilding()));
		row.setItemName(trim(row.getItemName()));
		row.setUnit(trim(row.getUnit()));
		row.setTemporaryReason(trim(row.getTemporaryReason()));

		if (row.getBuilding() != null && row.getBuilding().length() > 100) {
			throw new IllegalArgumentException(label + "作業内容は1〜100文字で入力してください。");
		}
		if (row.getItemName() == null || row.getItemName().length() > 100) {
			throw new IllegalArgumentException(label + "項目は1〜100文字で入力してください。");
		}
		if (row.getTemporaryReason() == null || row.getTemporaryReason().length() > 255) {
			throw new IllegalArgumentException(label + "依頼内容を入力してください（255文字以内）。");
		}
		if (row.getBaseQuantity() == null || row.getBaseQuantity() <= 0) {
			throw new IllegalArgumentException(label + "数量は1以上で入力してください。");
		}
		if (row.getUnit() == null || row.getUnit().length() > 20) {
			throw new IllegalArgumentException(label + "単位は1〜20文字で入力してください。");
		}
		if (row.getUnitPrice() == null || row.getUnitPrice().signum() < 0) {
			throw new IllegalArgumentException(label + "単価を0以上で入力してください。");
		}
	}

	private boolean isTemporary(BillingStatementItem item) {
		return item.getIsTemporary() != null && item.getIsTemporary() == 1;
	}

}
