package com.example.app.service;

import org.springframework.stereotype.Service;

import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserMapper userMapper;

	@Override
	public User getUserById(Integer id) {
		return userMapper.selectById(id);
	}
	
	@Override
  public User getUserByLoginId(String loginId) {
      return userMapper.selectByLoginId(loginId);
  }
}
