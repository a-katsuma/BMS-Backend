package com.example.app.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 緊急・追加作業。例外の案件（projects.order_route = 事前承認）に1件だけぶら下がる
// 区分は BillingStatementItem.WORK_TYPE_EMERGENCY／WORK_TYPE_ADDITIONAL を使う
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtraWork {

	public static final String STATUS_DRAFT = "下書き";
	public static final String STATUS_PENDING = "保留";
	public static final String STATUS_ORDERED = "受注済み";

	private Integer extraWorkId;
	private Integer projectId;
	private String workType;          // 緊急／追加
	private LocalDate workDate;       // 実施日
	private String location;          // 場所（任意。例：本館2F 201号室）
	private String content;           // 依頼内容
	private String requester;         // 依頼主（例：管理人の○○さん）
	private BigDecimal markupRate;    // 保存時点の事前承認の加算割合
	private BigDecimal taxRate;
	private String status;            // 下書き／保留／受注済み
	private LocalDateTime orderedAt;  // 受注した日時
	private String orderedBy;         // 受注した管理者
	private LocalDate billedDate;     // 請求済みにした日（単独の請求書で請求したとき。未請求は null）
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 表示用（集計）
	private BigDecimal subtotal;            // 当社→業者の合計（税別）
	private BigDecimal presentedSubtotal;   // 業者→顧客の合計（税別。行ごとに四捨五入して合計）
	private Integer importedStatementId;    // 取り込み先の毎次明細（未取り込みは null）
	private String importedBillingMonth;    // 取り込み先の請求年月

	// 表示用（全体の一覧。案件・顧客・業者）
	private String projectName;
	private Integer clientId;
	private String clientName;
	private Integer companyId;
	private String companyName;

	private List<ExtraWorkItem> items;

}
