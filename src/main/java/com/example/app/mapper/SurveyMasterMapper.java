package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.SurveyMaster;

@Mapper
public interface SurveyMasterMapper {

	// ①全件取得
	List<SurveyMaster> selectAll();

	// ②１件取得
	SurveyMaster selectById(Integer masterId);

	// ③重複チェック
	int countByTypeAndName(@Param("masterType") String masterType,
			@Param("name") String name,
			@Param("excludeId") Integer excludeId);

	// ④新規登録・編集・削除
	void insert(SurveyMaster master);

	void update(SurveyMaster master);

	void delete(Integer masterId);
	

	// ⑤種別ごとの一覧（表示順）
	List<SurveyMaster> selectByType(String masterType);

	// ⑥表示順の更新
	void updateDisplayOrder(@Param("masterId") Integer masterId,
			@Param("displayOrder") Integer displayOrder);

}
