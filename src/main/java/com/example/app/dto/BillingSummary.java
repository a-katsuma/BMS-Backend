package com.example.app.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

// 案件詳細の「請求状況」（管理者のみの項目は、業者には null）
@Data
public class BillingSummary {

	// 現況確認表
	private int surveyItemCount;        // 行数（0＝未作成）
	private int surveyPendingCount;     // 発注状況「未」の件数

	// ベース明細（使用中のベースの現在の版・表示順。空＝なし）
	private List<BaseVersion> bases = new ArrayList<>();
	private Integer surveyDiffCount;    // 現況確認表との差分の件数（案件全体・管理者のみ）

	// 毎次明細（業者は確定済みだけが対象）
	private String latestBillingMonth;  // 最新の請求年月 YYYY-MM（null＝なし）
	private String latestStatus;        // 下書き／確定
	private LocalDate latestIssuedDate; // 発行日（確定のみ）
	private Integer draftCount;         // 下書きの件数（管理者のみ）

	@Data
	@AllArgsConstructor
	public static class BaseVersion {
		private Integer baseId;
		private String baseName;
		private Integer versionNo;      // 現在の版
	}
}
