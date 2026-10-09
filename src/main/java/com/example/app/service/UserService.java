package com.example.app.service;

import java.util.List;

import com.example.app.domain.User;

public interface UserService {

	User getUserById(Integer id);

	User getUserByLoginId(String loginId);

	List<User> getUsersByCompanyId(Integer companyId);

	// ステータス（1:有効 / 0:無効）を指定して更新
	void updateStatus(Integer userId, int isActive);

	// ユーザー変更

	// ①パスワード再発行（管理者・代表ユーザーが代理で要求）
	void requestPasswordReset(Integer userId, String operatorName);

	// ①’パスワード再発行（本人が「パスワードを忘れた」から要求）
	void requestPasswordResetByLoginId(String loginId);

	// トークンから新パスワードを確定
	void confirmPasswordReset(String token, String newPassword);

	// 本人がログイン後に直接変更
	void changeOwnPassword(Integer userId, String currentPassword, String newPassword);

	// ②メールアドレス変更（管理者代理）
	void updateMasterEmail(Integer userId, String newEmail, String operatorName);

	// ②’メールアドレス変更（本人がログイン後）
	void updateOwnEmail(Integer userId, String currentPassword, String newEmail);

	// ③代表ユーザーの交代(操作：管理者)
	void transferMasterRole(Integer companyId, Integer newMasterUserId, String operatorName);

	// ユーザ名変更(操作：本人)
	void updateOwnName(Integer userId, String newName);

	// 一般ユーザー追加(操作：代表ユーザー)
	User addGeneralUser(Integer companyId, String name, String email, String operatorName);

	// アカウント発行時の招待（パスワード設定のリンクをメールで送る。業者の登録・一般ユーザーの追加で使う）
	void sendInvitation(User user);

}
