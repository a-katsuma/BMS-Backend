package com.example.app.domain;

import lombok.Data;

@Data
public class User {
	
	private Integer userId;
  private String name;
  private Integer companyId;
  private Integer roleFlag;

}
