package com.example.app.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingStatement {

	public static final String STATUS_DRAFT = "下書き";
	public static final String STATUS_CONFIRMED = "確定";

	private Integer statementId;
	private Integer baseId;
	private Integer versionId;        // コピー元のベースの版
	private String billingMonth;      // 請求年月（YYYY-MM）
	private BigDecimal taxRate;
	private String status;            // 下書き／確定
	private String createdBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String confirmedBy;
	private LocalDateTime confirmedAt;
	private LocalDate issuedDate;     // 発行日（確定時に入力）


	// 表示用（JOIN・集計）
	private Integer projectId;
	private Integer versionNo;
	private String baseName;          // コピー元のベース名
	private BigDecimal subtotal;      // 一覧用：税抜の小計

	private List<BillingStatementItem> items;
	private List<BillingStatementOtherItem> otherItems;
	private List<BillingStatementItem> temporaryItems; // 緊急・追加作業の行

}
