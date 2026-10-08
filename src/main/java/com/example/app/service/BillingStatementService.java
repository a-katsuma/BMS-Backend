package com.example.app.service;

import java.util.List;

import com.example.app.domain.BillingStatement;

public interface BillingStatementService {

	// ①一覧（confirmedOnly=true なら確定のみ）
	List<BillingStatement> getStatements(Integer projectId, boolean confirmedOnly);

	// ②1件（明細行・その他項目付き。別の案件の明細なら null）
	BillingStatement getStatement(Integer projectId, Integer statementId);

	// ③作成（指定したベースの現在の版をコピー。使用中のベースが1つだけなら baseId は省略可）→ 新しい明細ID
	Integer create(Integer projectId, Integer baseId, String billingMonth, String createdBy);

	// ④下書きの保存（調整数・実費）
	void save(Integer projectId, Integer statementId, BillingStatement form);

	// ⑤確定（保存してから確定）
	void confirm(Integer projectId, Integer statementId, BillingStatement form, String confirmedBy);

	// ⑥確定解除
	void unconfirm(Integer projectId, Integer statementId);

	// ⑦削除（下書きのみ）
	void delete(Integer projectId, Integer statementId);
}
