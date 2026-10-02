package com.example.app.service;

import java.math.BigDecimal;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;

public interface EmailService {

	// 新規業者登録時代表ユーザー初期アカウント発行
	void sendMasterUserWelcomeEmail(String toEmail, String rawPassword);

	// ユーザー変更時
	void sendPasswordResetLinkEmail(String toEmail, String resetUrl);
	void sendEmailChangedNotification(String oldEmail, String newEmail);
	void sendRoleTransferNotification(String newMasterEmail, String oldMasterEmail);

	// 一般ユーザー追加
	void sendGeneralUserWelcomeEmail(String toEmail, String rawPassword);

	//パスワード変更完了通知（現在のアドレス宛）
	void sendPasswordChangedNotification(String toEmail);
	

	// 緊急・追加作業の受注のお知らせ（業者の代表ユーザー宛）
	void sendExtraWorkOrderedNotification(String toEmail, Project project, ExtraWork work,
			BigDecimal presentedSubtotal, BigDecimal tax);




}
