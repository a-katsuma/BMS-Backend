package com.example.app.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilitySurveyItem {

	public static final String ORDER_STATUS_PENDING = "未";
	public static final String ORDER_STATUS_ORDERED = "済";

	private Integer surveyItemId;
	private Integer surveyId;
	private String building;          // 棟
	private String floorLabel;        // 階
	private String itemName;          // 項目名
	private Integer quantity;         // 数量
	private String unit;              // 単位
	private Integer excludedQuantity; // 対象外数
	private String excludedReason;    // 理由
	private String orderStatus;       // 発注状況（未／済）
	private Integer displayOrder;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
