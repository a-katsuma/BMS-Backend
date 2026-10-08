package com.example.app.service;

import java.util.List;

import com.example.app.domain.BaseDiffAlert;
import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingBaseItem;
import com.example.app.dto.BaseSaveRequest;
import com.example.app.dto.BillingSummary;

public interface BillingBaseService {

	// ①案件のベース一覧（表示順・明細なし。activeOnly=true なら使用中だけ）
	List<BillingBase> getBases(Integer projectId, boolean activeOnly);

	// ②1件（全版・明細付き。別の案件のベース・baseId が null なら null）
	BillingBase getBase(Integer projectId, Integer baseId);

	// ③新しいベースを生成できない理由（生成できるなら null）
	String checkGeneratable(Integer projectId);

	// ④現況確認表を「棟×項目×単位」で集計
	List<BillingBaseItem> buildPreview(Integer projectId);

	// ⑤ほかの使用中のベースの現在の版にある行（baseId=null なら使用中のすべてのベース）
	List<BillingBaseItem> getOtherBaseItems(Integer projectId, Integer baseId);

	// ⑥保存（baseId=null なら新しいベースを作成／訂正／改版）→ ベースID
	Integer save(Integer projectId, BaseSaveRequest request);

	// ⑦ベース名の変更
	void rename(Integer projectId, Integer baseId, String baseName);

	// ⑧使用停止（stopped=true）／再開
	void setStopped(Integer projectId, Integer baseId, boolean stopped);

	// ⑨削除（使用停止中で、毎次明細がないベースだけ）
	void delete(Integer projectId, Integer baseId);

	// ⑩現況確認表の発注状況（未／済）を、使用中のベースの現在の版と照合して付け直す
	void syncSurveyOrderStatus(Integer projectId);

	// ⑪現況確認表と使用中のベースに差がある案件（管理者ホーム用）
	List<BaseDiffAlert> getSurveyDiffAlerts();

	// ⑫案件詳細の請求状況（isAdmin=false なら業者向けの項目だけ）
	BillingSummary getBillingSummary(Integer projectId, boolean isAdmin);
}
