package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.User;

@Mapper
public interface UserMapper {

	User selectById(Integer id);

	User selectByLoginId(String loginId);

	// ★業者IDに紐づくユーザー一覧を取得（管理者→一般ユーザーの順）
	List<User> selectByCompanyId(Integer companyId);

	// ★ユーザーの有効/無効ステータスを更新
	void updateStatus(@Param("userId") Integer userId, @Param("isActive") Integer isActive);

	// マスターユーザー

	// 新規登録
	void insert(User user);

	// 削除
	void deleteByCompanyId(Integer companyId);
	
	// ユーザー変更
	void updatePassword(@Param("userId") Integer userId, @Param("password") String password);

	void updateLoginIdAndEmail(@Param("userId") Integer userId,
	        @Param("loginId") String loginId, @Param("email") String email);

	void updateRoleFlag(@Param("userId") Integer userId, @Param("roleFlag") Integer roleFlag);
	
	void updateName(@Param("userId") Integer userId, @Param("name") String name);

}
