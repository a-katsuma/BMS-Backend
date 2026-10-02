package com.example.app.dto;

import com.example.app.domain.ExtraWork;

import lombok.Data;

// 緊急・追加作業の新規受注（例外の案件と作業を一緒に作る）
@Data
public class ExtraWorkCreateRequest {
	private Integer clientId;
	private Integer companyId;
	private String projectName; // 空なら「【区分】MM/dd 場所（なければ依頼主）」
	private ExtraWork work;     // 作業の内容（行を含む）
}
