package com.example.app.service;

import java.util.List;

import com.example.app.domain.FacilitySurvey;
import com.example.app.domain.FacilitySurveyItem;

public interface FacilitySurveyService {

	// ①案件IDで取得（明細付き・未作成なら null）
	FacilitySurvey getSurveyByProjectId(Integer projectId);

	// ②保存（未作成なら新規作成、明細は行単位で追加・更新・削除）
	void saveItems(Integer projectId, List<FacilitySurveyItem> items);
}
