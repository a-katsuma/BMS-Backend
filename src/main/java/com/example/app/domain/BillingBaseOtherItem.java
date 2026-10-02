package com.example.app.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingBaseOtherItem {

	private Integer baseOtherItemId;
	private Integer baseId;
	private Integer versionId;
	private String feeName;              // 項目名（養生費など）
	private BigDecimal defaultUnitPrice; // 単価（実費は0）
	private Integer defaultQuantity;     // 数量
	private Integer isVariable;          // 1=毎回変動（実費）
	private Integer displayOrder;
}
