package com.example.app.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.app.domain.BillingBaseItem;
import com.example.app.domain.BillingBaseOtherItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseSaveRequest {

	public static final String MODE_CORRECT = "CORRECT"; // 訂正：今の版を上書き
	public static final String MODE_REVISE = "REVISE";   // 改版：新しい版を作成

private Integer baseId;   // null＝新しいベースを作成
	private String baseName;  // 新しいベースを作成するときだけ使う
	private String mode;
	private Integer quoteId;
	private BigDecimal taxRate;
	private List<BillingBaseItem> items;
	private List<BillingBaseOtherItem> otherItems;
}
