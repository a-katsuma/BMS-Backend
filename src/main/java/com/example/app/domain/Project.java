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

}
