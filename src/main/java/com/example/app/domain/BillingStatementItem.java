package com.example.app.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingStatementItem {

	private Integer statementItemId;
	private Integer statementId;
	private String building;            // 棟
	private String itemName;            // 項目名
	private String unit;                // 単位
	private Integer baseQuantity;       // 数量
	private Integer excludedQuantity;   // 対象外数
	private String excludedReason;      // 対象外の理由
	private Integer adjustmentQuantity; // 調整数（今回実施しなかった数）
	private String adjustmentReason;    // 調整理由（任意）
	private BigDecimal unitPrice;       // 単価
	private Integer displayOrder;

	// 緊急・追加作業（is_temporary = 1 の行。現況確認表・ベースには反映しない）
	public static final String WORK_TYPE_EMERGENCY = "緊急";
	public static final String WORK_TYPE_ADDITIONAL = "追加";

	private Integer isTemporary;
	private String workType;          // 緊急／追加
	private LocalDate workDate;       // 実施日
	private String temporaryReason;   // 依頼内容（緊急・追加作業の「依頼内容」と同じもの）
	private BigDecimal markupRate;    // 加算割合（保存時点の事前承認の値。承認なしは null）
	private Integer policyAgreed;     // 保存時点で事前承認が「承認する」だったか（1/0）
	private Integer extraWorkId;      // 取り込み元の緊急・追加作業（受注済みから取り込んだ行だけ）


}
