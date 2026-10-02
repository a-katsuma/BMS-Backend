package com.example.app.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.BaseDiffAlert;
import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingBaseItem;
import com.example.app.domain.BillingBaseOtherItem;
import com.example.app.domain.BillingBaseVersion;
import com.example.app.domain.BillingStatement;
import com.example.app.domain.FacilitySurvey;
import com.example.app.domain.FacilitySurveyItem;
import com.example.app.domain.Quote;
import com.example.app.dto.BaseSaveRequest;
import com.example.app.dto.BillingSummary;
import com.example.app.mapper.BillingBaseMapper;
import com.example.app.mapper.BillingStatementMapper;
import com.example.app.mapper.FacilitySurveyMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BillingBaseServiceImpl implements BillingBaseService {

	private static final String QUOTE_STATUS_ORDERED = "発注";
	private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("10.00");

	private final BillingBaseMapper baseMapper;
	private final FacilitySurveyMapper surveyMapper;
	private final QuoteService quoteService;
	private final BillingStatementMapper statementMapper; // 請求状況の集計用（Service だと循環参照になるため Mapper を使う）

	// ①案件IDで取得
	@Override
	public BillingBase getBaseByProjectId(Integer projectId) {
		BillingBase base = baseMapper.selectByProjectId(projectId);
		if (base == null) {
			return null;
		}
		List<BillingBaseVersion> versions = baseMapper.selectVersionsByBaseId(base.getBaseId());
		for (BillingBaseVersion v : versions) {
			v.setItems(baseMapper.selectItemsByVersionId(v.getVersionId()));
			v.setOtherItems(baseMapper.selectOtherItemsByVersionId(v.getVersionId()));
		}
		base.setVersions(versions);
		base.setCurrentVersion(versions.isEmpty() ? null : versions.get(0));
		return base;
	}

	// ②生成できない理由
	@Override
	public String checkGeneratable(Integer projectId) {
		if (baseMapper.selectByProjectId(projectId) != null) {
			return "ベースは既に生成されています。";
		}
		List<Quote> quotes = quoteService.getQuotesByProjectId(projectId);
		Quote latest = (quotes == null || quotes.isEmpty()) ? null : quotes.get(0);
		if (latest == null || !QUOTE_STATUS_ORDERED.equals(latest.getQuoteStatus())) {
			return "最新の見積りが「発注」になっていないため、ベースを生成できません。";
		}
		FacilitySurvey survey = surveyMapper.selectByProjectId(projectId);
		if (survey == null || surveyMapper.selectItemsBySurveyId(survey.getSurveyId()).isEmpty()) {
			return "現況確認表に項目がないため、ベースを生成できません。";
		}
		return null;
	}

	// ③現況確認表を「棟×項目×単位」で集計
	@Override
	public List<BillingBaseItem> buildPreview(Integer projectId) {
		FacilitySurvey survey = surveyMapper.selectByProjectId(projectId);
		if (survey == null) {
			return List.of();
		}

		Map<String, BillingBaseItem> grouped = new LinkedHashMap<>();
		Map<String, List<String>> reasons = new HashMap<>();

		for (FacilitySurveyItem s : surveyMapper.selectItemsBySurveyId(survey.getSurveyId())) {
			String key = BillingBaseItem.keyOf(s.getBuilding(), s.getItemName(), s.getUnit());

			BillingBaseItem g = grouped.computeIfAbsent(key, k -> {
				BillingBaseItem item = new BillingBaseItem();
				item.setBuilding(s.getBuilding());
				item.setItemName(s.getItemName());
				item.setUnit(s.getUnit());
				item.setBaseQuantity(0);
				item.setExcludedQuantity(0);
				return item;
			});
			g.setBaseQuantity(g.getBaseQuantity() + s.getQuantity());
			g.setExcludedQuantity(g.getExcludedQuantity() + s.getExcludedQuantity());

			// 対象外の理由は「階:理由(数)」でつなげる
			if (s.getExcludedQuantity() > 0) {
				reasons.computeIfAbsent(key, k -> new ArrayList<>())
						.add(s.getFloorLabel() + ":" + s.getExcludedReason() + "(" + s.getExcludedQuantity() + ")");
			}
		}

		int order = 1;
		for (Map.Entry<String, BillingBaseItem> e : grouped.entrySet()) {
			BillingBaseItem g = e.getValue();
			g.setDisplayOrder(order++);
			List<String> r = reasons.get(e.getKey());
			g.setExcludedReason(r == null ? null : limit(String.join(" / ", r), 255));
		}
		return new ArrayList<>(grouped.values());
	}

	// ④保存
	@Override
	public void save(Integer projectId, BaseSaveRequest request) {
		BigDecimal taxRate = validateTaxRate(request.getTaxRate());
		Integer quoteId = validateQuote(projectId, request.getQuoteId());
		List<BillingBaseItem> items = validateItems(request.getItems());
		List<BillingBaseOtherItem> others = validateOtherItems(request.getOtherItems());

		BillingBase base = baseMapper.selectByProjectId(projectId);

		if (base == null) {
			// 未生成：第1版を作成
			String blockReason = checkGeneratable(projectId);
			if (blockReason != null) {
				throw new IllegalStateException(blockReason);
			}
			base = new BillingBase();
			base.setProjectId(projectId);
			baseMapper.insert(base);
			createVersion(base.getBaseId(), 1, quoteId, taxRate, items, others);

		} else {
			List<BillingBaseVersion> versions = baseMapper.selectVersionsByBaseId(base.getBaseId());

			if (versions.isEmpty()) {
				createVersion(base.getBaseId(), 1, quoteId, taxRate, items, others);

			} else if (BaseSaveRequest.MODE_CORRECT.equals(request.getMode())) {
				// 訂正：今の版を上書き
				BillingBaseVersion current = versions.get(0);
				current.setQuoteId(quoteId);
				current.setTaxRate(taxRate);
				baseMapper.updateVersion(current);
				baseMapper.deleteItemsByVersionId(current.getVersionId());
				baseMapper.deleteOtherItemsByVersionId(current.getVersionId());
				insertLines(current, items, others);

			} else if (BaseSaveRequest.MODE_REVISE.equals(request.getMode())) {
				// 改版：新しい版を作成
				int nextNo = versions.get(0).getVersionNo() + 1;
				createVersion(base.getBaseId(), nextNo, quoteId, taxRate, items, others);

			} else {
				throw new IllegalArgumentException("保存方法が不正です。");
			}
		}

		syncSurveyOrderStatus(projectId);
	}

	// ⑤現況確認表の発注状況を付け直す
	@Override
	public void syncSurveyOrderStatus(Integer projectId) {
		FacilitySurvey survey = surveyMapper.selectByProjectId(projectId);
		if (survey == null) {
			return;
		}

		// ベースの現在の版を「棟＋項目＋単位」で引けるようにする
		Map<String, BillingBaseItem> baseMap = new HashMap<>();
		BillingBase base = baseMapper.selectByProjectId(projectId);
		if (base != null) {
			List<BillingBaseVersion> versions = baseMapper.selectVersionsByBaseId(base.getBaseId());
			if (!versions.isEmpty()) {
				for (BillingBaseItem item : baseMapper.selectItemsByVersionId(versions.get(0).getVersionId())) {
					baseMap.put(item.groupKey(), item);
				}
			}
		}

		// 集計（数量・対象外数）がベースと一致している組み合わせは「済」
		Set<String> matched = new HashSet<>();
		for (BillingBaseItem agg : buildPreview(projectId)) {
			BillingBaseItem b = baseMap.get(agg.groupKey());
			if (b != null
					&& Objects.equals(b.getBaseQuantity(), agg.getBaseQuantity())
					&& Objects.equals(b.getExcludedQuantity(), agg.getExcludedQuantity())) {
				matched.add(agg.groupKey());
			}
		}

		for (FacilitySurveyItem s : surveyMapper.selectItemsBySurveyId(survey.getSurveyId())) {
			String key = BillingBaseItem.keyOf(s.getBuilding(), s.getItemName(), s.getUnit());
			String status = matched.contains(key)
					? FacilitySurveyItem.ORDER_STATUS_ORDERED
					: FacilitySurveyItem.ORDER_STATUS_PENDING;
			if (!status.equals(s.getOrderStatus())) {
				surveyMapper.updateOrderStatus(s.getSurveyItemId(), status);
			}
		}
	}
	

	// ⑥現況確認表とベースの現在の版に差がある案件
	@Override
	public List<BaseDiffAlert> getSurveyDiffAlerts() {
		List<BaseDiffAlert> alerts = new ArrayList<>();
		for (BaseDiffAlert target : baseMapper.selectAlertTargets()) {
			if (target.getCurrentVersionId() == null) {
				continue;
			}
			
			int count = countSurveyDiffs(target.getProjectId(), target.getCurrentVersionId());
			if (count > 0) {
				target.setDiffCount(count);
				alerts.add(target);
			}
		}
		return alerts;
	}
	
	// ⑦案件詳細の請求状況（isAdmin=false なら業者向けの項目だけ）
	@Override
	public BillingSummary getBillingSummary(Integer projectId, boolean isAdmin) {
		BillingSummary summary = new BillingSummary();

		// 現況確認表
		FacilitySurvey survey = surveyMapper.selectByProjectId(projectId);
		if (survey != null) {
			List<FacilitySurveyItem> items = surveyMapper.selectItemsBySurveyId(survey.getSurveyId());
			summary.setSurveyItemCount(items.size());
			summary.setSurveyPendingCount((int) items.stream()
					.filter(s -> !FacilitySurveyItem.ORDER_STATUS_ORDERED.equals(s.getOrderStatus()))
					.count());
		}

		// ベース明細（現在の版）
		BillingBase base = baseMapper.selectByProjectId(projectId);
		if (base != null) {
			List<BillingBaseVersion> versions = baseMapper.selectVersionsByBaseId(base.getBaseId());
			if (!versions.isEmpty()) {
				BillingBaseVersion current = versions.get(0);
				summary.setCurrentVersionNo(current.getVersionNo());
				if (isAdmin) {
					summary.setSurveyDiffCount(countSurveyDiffs(projectId, current.getVersionId()));
				}
			}
		}

		// 毎次明細（新しい年月順。業者は確定済みだけ）
		List<BillingStatement> statements = statementMapper.selectByProjectId(projectId, !isAdmin);
		if (!statements.isEmpty()) {
			BillingStatement latest = statements.get(0);
			summary.setLatestBillingMonth(latest.getBillingMonth());
			summary.setLatestStatus(latest.getStatus());
			summary.setLatestIssuedDate(latest.getIssuedDate());
		}
		if (isAdmin) {
			summary.setDraftCount((int) statements.stream()
					.filter(s -> BillingStatement.STATUS_DRAFT.equals(s.getStatus()))
					.count());
		}
		return summary;
	}



	// 差分の件数（画面の countDiffs と同じ数え方：数量・対象外数の違い＋現況になし＋ベース未登録）
	private int countSurveyDiffs(Integer projectId, Integer versionId) {
		Map<String, BillingBaseItem> surveyMap = new HashMap<>();
		for (BillingBaseItem agg : buildPreview(projectId)) {
			surveyMap.put(agg.groupKey(), agg);
		}

		int count = 0;
		Set<String> baseKeys = new HashSet<>();
		for (BillingBaseItem b : baseMapper.selectItemsByVersionId(versionId)) {
			baseKeys.add(b.groupKey());
			BillingBaseItem s = surveyMap.get(b.groupKey());
			if (s == null
					|| !Objects.equals(s.getBaseQuantity(), b.getBaseQuantity())
					|| !Objects.equals(s.getExcludedQuantity(), b.getExcludedQuantity())) {
				count++;
			}
		}
		for (String key : surveyMap.keySet()) {
			if (!baseKeys.contains(key)) {
				count++;
			}
		}
		return count;
	}


	// ---------- 版の作成・明細の登録 ----------

	private void createVersion(Integer baseId, int versionNo, Integer quoteId, BigDecimal taxRate,
			List<BillingBaseItem> items, List<BillingBaseOtherItem> others) {
		BillingBaseVersion version = new BillingBaseVersion();
		version.setBaseId(baseId);
		version.setVersionNo(versionNo);
		version.setQuoteId(quoteId);
		version.setTaxRate(taxRate);
		baseMapper.insertVersion(version);
		insertLines(version, items, others);
	}

	private void insertLines(BillingBaseVersion version, List<BillingBaseItem> items,
			List<BillingBaseOtherItem> others) {
		for (int i = 0; i < items.size(); i++) {
			BillingBaseItem item = items.get(i);
			item.setBaseItemId(null); // 前の版のIDは使わず新しく採番
			item.setBaseId(version.getBaseId());
			item.setVersionId(version.getVersionId());
			item.setDisplayOrder(i + 1);
			baseMapper.insertItem(item);
		}
		for (int i = 0; i < others.size(); i++) {
			BillingBaseOtherItem other = others.get(i);
			other.setBaseOtherItemId(null);
			other.setBaseId(version.getBaseId());
			other.setVersionId(version.getVersionId());
			other.setDisplayOrder(i + 1);
			baseMapper.insertOtherItem(other);
		}
	}

	// ---------- 入力チェック ----------

	private BigDecimal validateTaxRate(BigDecimal taxRate) {
		BigDecimal rate = (taxRate == null) ? DEFAULT_TAX_RATE : taxRate;
		if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) >= 0) {
			throw new IllegalArgumentException("税率が不正です。");
		}
		return rate;
	}

	private Integer validateQuote(Integer projectId, Integer quoteId) {
		if (quoteId == null) {
			return null;
		}
		Quote quote = quoteService.getQuoteById(quoteId);
		if (quote == null || !Objects.equals(quote.getProjectId(), projectId)) {
			throw new IllegalArgumentException("指定された見積りはこの案件のものではありません。");
		}
		return quoteId;
	}

	private List<BillingBaseItem> validateItems(List<BillingBaseItem> items) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("明細を1件以上入力してください。");
		}

		Set<String> keys = new HashSet<>();
		for (int i = 0; i < items.size(); i++) {
			BillingBaseItem item = items.get(i);
			item.setBuilding(trim(item.getBuilding()));
			item.setItemName(trim(item.getItemName()));
			item.setUnit(trim(item.getUnit()));
			item.setExcludedReason(trim(item.getExcludedReason()));

			if (isBlank(item.getBuilding()) || isBlank(item.getItemName()) || isBlank(item.getUnit())) {
				throw new IllegalArgumentException((i + 1) + "行目：棟・項目・単位は必須です。");
			}
			String label = item.getBuilding() + " " + item.getItemName() + "：";

			if (item.getBuilding().length() > 50 || item.getItemName().length() > 100 || item.getUnit().length() > 20) {
				throw new IllegalArgumentException(label + "棟は50文字、項目は100文字、単位は20文字以内で入力してください。");
			}
			if (!keys.add(item.groupKey())) {
				throw new IllegalArgumentException(label + "同じ棟・項目・単位の行が重複しています。");
			}
			if (item.getBaseQuantity() == null || item.getBaseQuantity() < 0) {
				throw new IllegalArgumentException(label + "数量は0以上で入力してください。");
			}

			int excluded = (item.getExcludedQuantity() == null) ? 0 : item.getExcludedQuantity();
			if (excluded < 0 || excluded > item.getBaseQuantity()) {
				throw new IllegalArgumentException(label + "対象外数は0〜数量の範囲で入力してください。");
			}
			if (excluded > 0 && isBlank(item.getExcludedReason())) {
				throw new IllegalArgumentException(label + "対象外数がある場合は理由を入力してください。");
			}
			item.setExcludedQuantity(excluded);
			item.setExcludedReason(excluded == 0 ? null : limit(item.getExcludedReason(), 255));

			if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
				throw new IllegalArgumentException(label + "単価を0以上で入力してください。");
			}
		}
		return items;
	}

	private List<BillingBaseOtherItem> validateOtherItems(List<BillingBaseOtherItem> others) {
		if (others == null) {
			return List.of();
		}
		for (int i = 0; i < others.size(); i++) {
			BillingBaseOtherItem other = others.get(i);
			String name = trim(other.getFeeName());
			String label = "その他項目 " + (i + 1) + "行目：";

			if (isBlank(name) || name.length() > 50) {
				throw new IllegalArgumentException(label + "項目名は1〜50文字で入力してください。");
			}
			boolean isVariable = other.getIsVariable() != null && other.getIsVariable() == 1;
			if (isVariable) {
				other.setDefaultUnitPrice(BigDecimal.ZERO); // 実費：単価は明細作成時に入力
			} else if (other.getDefaultUnitPrice() == null || other.getDefaultUnitPrice().signum() < 0) {
				throw new IllegalArgumentException(label + "単価を0以上で入力してください。");
			}
			if (other.getDefaultQuantity() == null || other.getDefaultQuantity() < 0) {
				throw new IllegalArgumentException(label + "数量を0以上で入力してください。");
			}
			other.setFeeName(name);
			other.setIsVariable(isVariable ? 1 : 0);
		}
		return others;
	}

	private String trim(String s) {
		return (s == null) ? null : s.trim();
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private String limit(String s, int max) {
		return (s.length() <= max) ? s : s.substring(0, max);
	}
}
