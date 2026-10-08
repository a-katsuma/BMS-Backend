package com.example.app.domain;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingBase {

	private Integer baseId;
	private Integer projectId;
	private String baseName;          // ベース名
	private Integer displayOrder;     // 表示順
	private LocalDateTime stoppedAt;  // 使用停止日時（null＝使用中）
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 表示用（一覧の集計）
	private Integer currentVersionNo; // 現在の版の番号（null＝版なし）
	private Integer statementCount;   // 毎次明細の件数（削除できるかの判定用）

	// 現在の版（＝versions の先頭）
	private BillingBaseVersion currentVersion;
	// 全版（新しい順・明細付き）
	private List<BillingBaseVersion> versions;

	// 使用停止中か（JSON では "stopped"）
	public boolean isStopped() {
		return stoppedAt != null;
	}
}
