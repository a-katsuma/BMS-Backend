package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.app.domain.Document;

@Mapper
public interface DocumentMapper {

	// 閲覧（全件取得：案件IDまたは顧客IDで絞り込むのが一般的）
	// すべての資料を出す場合は selectAll()
	List<Document> selectByProjectId(Integer projectId);

	List<Document> selectByClientId(Integer clientId);

	// ②１件取得(編集用)
	Document selectById(Integer docId);

	// ⑤新規登録
	void insert(Document document);

	// ⑥編集
	void update(Document document);

	// ⑦削除
	void delete(Integer docId);
}
