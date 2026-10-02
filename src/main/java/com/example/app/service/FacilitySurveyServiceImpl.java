package com.example.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.FacilitySurvey;
import com.example.app.domain.FacilitySurveyItem;
import com.example.app.mapper.FacilitySurveyMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FacilitySurveyServiceImpl implements FacilitySurveyService {

	private final FacilitySurveyMapper surveyMapper;
	private final BillingBaseService billingBaseService;

	// ①案件IDで取得（明細付き）
	@Override
	public FacilitySurvey getSurveyByProjectId(Integer projectId) {
		FacilitySurvey survey = surveyMapper.selectByProjectId(projectId);
		if (survey != null) {
			survey.setItems(surveyMapper.selectItemsBySurveyId(survey.getSurveyId()));
		}
		return survey;
	}

	// ②保存
	@Override
	public void saveItems(Integer projectId, List<FacilitySurveyItem> items) {
		FacilitySurvey survey = surveyMapper.selectByProjectId(projectId);
		if (survey == null) {
			survey = new FacilitySurvey();
			survey.setProjectId(projectId);
			surveyMapper.insert(survey);
		}
		Integer surveyId = survey.getSurveyId();

		// 既存行をIDで引けるようにする
		Map<Integer, FacilitySurveyItem> existingMap = new HashMap<>();
		for (FacilitySurveyItem existing : surveyMapper.selectItemsBySurveyId(surveyId)) {
			existingMap.put(existing.getSurveyItemId(), existing);
		}

		List<FacilitySurveyItem> list = (items == null) ? List.of() : items;
		for (int i = 0; i < list.size(); i++) {
			FacilitySurveyItem item = list.get(i);
			validateItem(item, i + 1);
			item.setSurveyId(surveyId);
			item.setDisplayOrder(i + 1);

			// 新規行は「未」
			if (item.getSurveyItemId() == null) {
				item.setOrderStatus(FacilitySurveyItem.ORDER_STATUS_PENDING);
				surveyMapper.insertItem(item);
				continue;
			}

			FacilitySurveyItem existing = existingMap.remove(item.getSurveyItemId());
			if (existing == null) {
				throw new IllegalArgumentException("対象の行が見つかりません。画面を再読み込みしてください。");
			}

			// ベースに影響する内容が変わったら「未」に戻す（画面から送られた発注状況は使わない）
			item.setOrderStatus(isOrderChanged(existing, item)
					? FacilitySurveyItem.ORDER_STATUS_PENDING
					: existing.getOrderStatus());
			surveyMapper.updateItem(item);
		}

		// 画面から消えた行は削除
		for (Integer removedId : existingMap.keySet()) {
			surveyMapper.deleteItem(removedId);
		}

		surveyMapper.touch(surveyId);
		// ベース（現在の版）と照合して発注状況を付け直す
		billingBaseService.syncSurveyOrderStatus(projectId);
	}

	// ベース（棟×項目の合計）に影響する変更か
	private boolean isOrderChanged(FacilitySurveyItem before, FacilitySurveyItem after) {
		return !Objects.equals(before.getBuilding(), after.getBuilding())
				|| !Objects.equals(before.getItemName(), after.getItemName())
				|| !Objects.equals(before.getUnit(), after.getUnit())
				|| !Objects.equals(before.getQuantity(), after.getQuantity())
				|| !Objects.equals(before.getExcludedQuantity(), after.getExcludedQuantity());
	}

	// 入力チェック
	private void validateItem(FacilitySurveyItem item, int rowNo) {
		item.setBuilding(trim(item.getBuilding()));
		item.setFloorLabel(trim(item.getFloorLabel()));
		item.setItemName(trim(item.getItemName()));
		item.setUnit(trim(item.getUnit()));
		item.setExcludedReason(trim(item.getExcludedReason()));

		String prefix = describe(item, rowNo);

		if (isBlank(item.getBuilding()) || isBlank(item.getFloorLabel()) || isBlank(item.getItemName())) {
			throw new IllegalArgumentException(prefix + "棟・階・項目名は必須です。");
		}
		if (isBlank(item.getUnit())) {
			throw new IllegalArgumentException(prefix + "単位は必須です。");
		}
		if (item.getQuantity() == null || item.getQuantity() < 0) {
			throw new IllegalArgumentException(prefix + "数量は0以上で入力してください。");
		}

		int excluded = (item.getExcludedQuantity() == null) ? 0 : item.getExcludedQuantity();
		if (excluded < 0 || excluded > item.getQuantity()) {
			throw new IllegalArgumentException(prefix + "対象外数は0〜数量の範囲で入力してください。");
		}
		if (excluded > 0 && isBlank(item.getExcludedReason())) {
			throw new IllegalArgumentException(prefix + "対象外数がある場合は理由を入力してください。");
		}

		item.setExcludedQuantity(excluded);
		if (excluded == 0) {
			item.setExcludedReason(null);
		}
	}

	// エラー表示用の行の説明（例：「1号棟 1F 空調機：」）
	private String describe(FacilitySurveyItem item, int rowNo) {
		if (isBlank(item.getBuilding()) || isBlank(item.getFloorLabel()) || isBlank(item.getItemName())) {
			return rowNo + "行目：";
		}
		return item.getBuilding() + " " + item.getFloorLabel() + " " + item.getItemName() + "：";
	}

	private String trim(String s) {
		return (s == null) ? null : s.trim();
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
