package com.example.app.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Getter/Setter、toStringなどが自動生成
@NoArgsConstructor // MyBatisの不具合防止のお守り
@AllArgsConstructor // 全データ詰め込み用コンストラクタ
public class Company {

	private Integer companyId;

	@NotBlank
	@Size(max = 30)
	private String companyName; // company_name (発注業者名)

	@NotBlank
	@Size(max = 100)
	private String companyKana;

	@NotBlank
	@Size(min = 7, max = 7)
	private String companyPostalcode; // company_postalcode(発注業者郵便番号)

	@NotBlank
	@Size(max = 100)
	private String companyAddress; // company_address(発注業者住所)

	@NotBlank
	@Size(min = 10, max = 11)
	private String companyPhone; // phone_number（発注業者電話番号）

	// ハイフンあり整形後の値を保持するフィールド
	private String formattedCompanyPostalcode;
	private String formattedCompanyPhone;
}
