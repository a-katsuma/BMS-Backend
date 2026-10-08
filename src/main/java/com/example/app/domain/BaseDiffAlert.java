package com.example.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 現況確認表とベースの現在の版に差がある案件（管理者ホーム用）
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseDiffAlert {

	private Integer projectId;
	private String projectName;
	private String clientName;
	private String companyName;
	private Integer diffCount;
}
