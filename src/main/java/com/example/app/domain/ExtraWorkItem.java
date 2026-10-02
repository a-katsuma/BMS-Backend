package com.example.app.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtraWorkItem {

	private Integer extraWorkItemId;
	private Integer extraWorkId;
	private String itemName;        // 項目名
	private Integer quantity;       // 数量
	private String unit;            // 単位
	private BigDecimal unitPrice;   // 当社単価
	private Integer displayOrder;

}
