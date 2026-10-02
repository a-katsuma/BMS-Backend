package com.example.app.domain;

import lombok.Data;

@Data
public class User {

	private Integer userId;
	private String loginId;
	private String password;
	private String name;
	private Integer companyId;
	private Integer roleFlag;
	
	private String email;
	private Integer isActive;
	

	// 会社名(userテーブルからJOIN用)
	private String companyName;

}
