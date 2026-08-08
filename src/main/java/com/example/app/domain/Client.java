package com.example.app.domain;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {

	//顧客ID（自動採番用）
	private Integer clientId;

	// 顧客名
	@NotBlank
	@Size(max = 30)
	private String clientName;

	// 顧客名カナ
	@NotBlank
	@Size(max = 100)
	private String clientKana;

	// 郵便番号
	@NotBlank
	@Size(min = 7, max = 7)
	private String clientPostalcode;

	// 住所
	@NotBlank
	@Size(max = 100)
	private String clientAddress;

	// 電話番号
	@NotBlank
	// 日本の電話番号はハイフンなしで10〜11桁
	// ハイフンありで12〜13桁なので、間をとって10〜13桁で制限します
	@Size(min = 10, max = 11)
	private String clientPhone;

	// 最終更新日取得
	private LocalDateTime updatedAt;

	// ハイフンあり整形後の値を保持するフィールド（DBには保存されないので注意）
	private String formattedClientPostalcode;
	private String formattedClientPhone;

}
