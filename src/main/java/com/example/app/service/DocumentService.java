package com.example.app.service;

import java.util.List;

import com.example.app.domain.Document;

public interface DocumentService {

	// ①案件IDでリスト取得
	List<Document> getDocumentsByProjectId(Integer projectId);

	// ①顧客IDでリスト取得
	List<Document> getDocumentsByClientId(Integer clientId);

	// ②編集の準備用（IDで1件取得）
	Document getDocumentById(Integer docId);

	// ⑤新規登録
	void addDocument(Document document);

	// ⑥編集）
	void editDocument(Document document);

	// ⑦削除
	void deleteDocument(Integer docId);

}
