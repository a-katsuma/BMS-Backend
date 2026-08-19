package com.example.app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.app.domain.User;

@Mapper
public interface UserMapper {
	
	User selectById(Integer id);
	User selectByLoginId(String loginId);

}
