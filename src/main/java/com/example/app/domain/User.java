package com.example.app.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class User {

	private Integer userId;
	private String loginId;
	// private String password;

	// 受け取る（ログインのリクエスト）だけで、応答の JSON には出さない（ハッシュを画面に送らないため）
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
	private String name;
	private Integer companyId;
	private Integer roleFlag;

	private String email;
	private Integer isActive;


	// 会社名(userテーブルからJOIN用)
	private String companyName;

}
