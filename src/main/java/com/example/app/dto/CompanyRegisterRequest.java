package com.example.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRegisterRequest {
	
	// --- 業者情報 ---
		@NotBlank
		@Size(max = 30)
		private String companyName;

		@NotBlank
		@Size(max = 100)
		private String companyKana;

		@NotBlank
		@Size(min = 7, max = 7)
		private String companyPostalcode;

		@NotBlank
		@Size(max = 100)
		private String companyAddress;

		@NotBlank
		@Size(min = 10, max = 11)
		private String companyPhone;

		// --- マスターユーザー情報 ---
		@NotBlank
		@Size(max = 50)
		private String masterName;

		@NotBlank
		@Email
		@Size(max = 100)
		private String masterEmail;

}
