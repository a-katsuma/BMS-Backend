package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.FacilitySurvey;
import com.example.app.domain.FacilitySurveyItem;

@Mapper
public interface FacilitySurveyMapper {

	// ①案件IDで1件取得
	FacilitySurvey selectByProjectId(Integer projectId);

	// ②明細一覧取得
	List<FacilitySurveyItem> selectItemsBySurveyId(Integer surveyId);

	// ③新規登録
	void insert(FacilitySurvey survey);

	// ④更新日時のみ更新
	void touch(Integer surveyId);

	// ⑤明細の追加・更新・削除
	void insertItem(FacilitySurveyItem item);

	void updateItem(FacilitySurveyItem item);

	void deleteItem(Integer surveyItemId);

	// ⑥全行を「済」にする（ベース生成時）
	void markAllOrdered(Integer surveyId);

	// ⑦発注状況を1行更新（ベースとの照合結果）
	void updateOrderStatus(@Param("surveyItemId") Integer surveyItemId,
			@Param("orderStatus") String orderStatus);
 }

