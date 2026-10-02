package com.example.app.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingStatementOtherItem {

	private Integer statementOtherItemId;
	private Integer statementId;
	private String feeName;       // 項目名
	private Integer quantity;     // 数量
	private BigDecimal unitPrice; // 単価
	private Integer isVariable;   // 1=実費（毎回変動）
	private Integer displayOrder;
}
