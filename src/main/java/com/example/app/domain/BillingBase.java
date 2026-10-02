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
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 現在の版（＝versions の先頭）
	private BillingBaseVersion currentVersion;
	// 全版（新しい順・明細付き）
	private List<BillingBaseVersion> versions;
}
