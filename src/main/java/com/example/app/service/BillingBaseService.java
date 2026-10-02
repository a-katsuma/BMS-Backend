package com.example.app.service;

import java.util.List;

import com.example.app.domain.BaseDiffAlert;
import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingBaseItem;
import com.example.app.dto.BaseSaveRequest;
import com.example.app.dto.BillingSummary;

public interface BillingBaseService {

	// ①案件IDで取得（全版・明細付き・未生成なら null）
	BillingBase getBaseByProjectId(Integer projectId);

	// ②生成できない理由（生成できるなら null）
	String checkGeneratable(Integer projectId);

	// ③現況確認表を「棟×項目×単位」で集計
	List<BillingBaseItem> buildPreview(Integer projectId);

	// ④保存（未生成なら第1版を作成／訂正／改版）
	void save(Integer projectId, BaseSaveRequest request);

	// ⑤現況確認表の発注状況（未／済）をベースの現在の版と照合して付け直す
	void syncSurveyOrderStatus(Integer projectId);
	
	// ⑥現況確認表とベースの現在の版に差がある案件（管理者ホーム用）
	List<BaseDiffAlert> getSurveyDiffAlerts();
	
	//　⑦案件詳細の請求状況（isAdmin=false なら業者向けの項目だけ）
	BillingSummary getBillingSummary(Integer projectId, boolean isAdmin);

}
