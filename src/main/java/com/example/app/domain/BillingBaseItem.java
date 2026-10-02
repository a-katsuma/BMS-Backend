package com.example.app.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingBaseItem {

	private Integer baseItemId;
	private Integer baseId;
	private Integer versionId;
	private String building;          // 棟
	private String itemName;          // 項目名
	private Integer baseQuantity;     // 数量
	private String unit;              // 単位
	private BigDecimal unitPrice;     // 単価
	private Integer excludedQuantity; // 対象外数
	private String excludedReason;    // 理由
	private Integer displayOrder;
	private Integer isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 集計・照合用のキー（棟＋項目＋単位）
	public static String keyOf(String building, String itemName, String unit) {
		return building + "\u0000" + itemName + "\u0000" + unit;
	}

	public String groupKey() {
		return keyOf(building, itemName, unit);
	}
}
