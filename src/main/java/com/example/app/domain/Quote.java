package com.example.app.domain;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

	private Integer quoteId;
	private Integer projectId;

	private LocalDate quoteDate;

	private String quoteFilepath;

	@NotEmpty
	private String quoteStatus;

	private List<Quote> historyList;

	private java.time.LocalDateTime deletedAt; // 論理削除日時（null＝有効）

	// 必要に応じて案件情報を保持するためのフィールド（JOIN用）
	// private Project project;

	// 見積り判定
	private String judgeUser;
	// 見積り判定ユーザーID(FK)
	private Integer judgeUserId;

	// 見積り期限
	private LocalDate deadlineDate;
	private String clientName;
	private String projectName;
	private String companyName;
	private String latestQuoteStatus;

	// 表示用
	private Boolean quoteDeleted;      // 判定履歴：その見積りが削除済みか
	private Integer baseVersionCount;  // 削除済みの見積り：ベース明細の版からの参照数（1以上なら完全には削除できない）

}