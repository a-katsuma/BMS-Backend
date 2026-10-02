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
public class BillingBaseVersion {

	private Integer versionId;
	private Integer baseId;
	private Integer versionNo;
	private Integer quoteId;       // 元になった見積り（任意）
	private BigDecimal taxRate;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 見積り（表示用・JOIN）
	private LocalDate quoteDate;
	private String quoteFilepath;
	private String quoteStatus;

	private List<BillingBaseItem> items;
	private List<BillingBaseOtherItem> otherItems;
}
