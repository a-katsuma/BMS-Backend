package com.example.app.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyMaster {

	public static final String TYPE_ITEM = "ITEM";   // 項目
	public static final String TYPE_UNIT = "UNIT";   // 単位
	public static final String TYPE_OTHER = "OTHER"; // その他項目（ベース用）

	private Integer masterId;
	private String masterType;
	private String name;
	private String defaultUnit;          // ITEM用
	private Integer displayOrder;
	private LocalDateTime createdAt;
}
