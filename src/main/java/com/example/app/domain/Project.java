package com.example.app.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
	

	// 受注の経路（見積り：通常の案件／事前承認：緊急・追加作業の例外の案件）
	public static final String ORDER_ROUTE_QUOTE = "見積り";
	public static final String ORDER_ROUTE_POLICY = "事前承認";


	private Integer projectId;

	@NotNull(message = "{projectForm.companyId.NotNull}")
	private Integer companyId;

	@NotNull(message = "{projectForm.clientId.NotNull}")
	private Integer clientId;

	private LocalDate registrationDate; // java.time.LocalDateを使用

	@NotBlank
	@Size(max = 30)
	private String projectName;

	@NotBlank(message = "{projectForm.contractType.NotBlank}")
	private String contractType;
	private String orderRoute; // 受注の経路（見積り／事前承認）。登録時に null なら「見積り」


	@NotBlank(message = "{projectForm.status.NotBlank}")
	private String status;

	@NotBlank(message = "{projectForm.projectStaffname.NotBlank}")
	@Size(max = 30, message = "{projectForm.projectStaffname.Size}")
	private String projectStaffname;

	@Size(max = 200, message = "{projectForm.projectRemarks.Size}")
	private String projectRemarks;

	private String latestQuoteStatus;

	// 結合して取得する用の名前フィールド（必要に応じて）
	private String companyName;
	private String clientName;
	private LocalDate deadlineDate;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt; // 論理削除の日時（NULL＝削除されていない）

}
