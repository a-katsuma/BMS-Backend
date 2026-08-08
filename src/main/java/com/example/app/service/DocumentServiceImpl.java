package com.example.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Document;
import com.example.app.mapper.DocumentMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional // 途中で1個でも失敗したらロールバック
@RequiredArgsConstructor // finalがついたMapperを自動でドッキング
public class DocumentServiceImpl implements DocumentService {

	private final DocumentMapper documentMapper;

	//	①IDでリスト取得
	@Override
	public List<Document> getDocumentsByProjectId(Integer projectId) {
		return documentMapper.selectByProjectId(projectId);
	}

	@Override
	public List<Document> getDocumentsByClientId(Integer clientId) {
		return documentMapper.selectByClientId(clientId);
	}

	// ②１件取得(編集用)
	@Override
	public Document getDocumentById(Integer docId) {
		return documentMapper.selectById(docId);
	}

	// ⑤新規登録
	@Override
	public void addDocument(Document document) {
		// ここでDB保存前の最終処理（現在日時のセットなど）を行う
		if (document.getDocCreatedAt() == null) {
			document.setDocCreatedAt(LocalDateTime.now());
		}

		documentMapper.insert(document);
	}

	// ⑥編集
	@Override
	public void editDocument(Document document) {
		documentMapper.update(document);
	}

	// ⑦削除
	@Override
	public void deleteDocument(Integer docId) {
		documentMapper.delete(docId);
	}

}