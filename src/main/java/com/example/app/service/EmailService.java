package com.example.app.service;

import java.math.BigDecimal;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;

public interface EmailService {

	// アカウント発行時の招待：パスワード設定のリンク（業者の代表・一般ユーザー共通。roleLabel は「代表ユーザー」など）
	void sendInvitationEmail(String toEmail, String roleLabel, String setupUrl, int validHours);

	// ユーザー変更時
	void sendPasswordResetLinkEmail(String toEmail, String resetUrl);
	void sendEmailChangedNotification(String oldEmail, String newEmail);
	void sendRoleTransferNotification(String newMasterEmail, String oldMasterEmail);


	//パスワード変更完了通知（現在のアドレス宛）
	void sendPasswordChangedNotification(String toEmail);


	// 緊急・追加作業の受注のお知らせ（業者の代表ユーザー宛）
	void sendExtraWorkOrderedNotification(String toEmail, Project project, ExtraWork work,
			BigDecimal presentedSubtotal, BigDecimal tax);




}
