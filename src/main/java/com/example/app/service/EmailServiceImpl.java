package com.example.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	// モック化（実送信停止）中のためフィールドを残したままログ出力のみ
	@SuppressWarnings("unused")
	private final JavaMailSender mailSender;

	// 新規業者登録時代表ユーザー初期アカウント発行
	@Override
	public void sendMasterUserWelcomeEmail(String toEmail, String rawPassword) {
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(toEmail);
		//		message.setSubject("【重要】○○システムアカウント発行と初期パスワードのお知らせ");
		//		message.setText(
		//
		//				"○○システムへ、代表ユーザーアカウントの登録が完了いたしました。\n\n" +
		//						"以下のログイン情報でシステムへログインし、パスワードの変更をお願いいたします。。\n\n" +
		//						"----------------------------------------\n" +
		//						"ログインID (メールアドレス): " + toEmail + "\n" +
		//						"初期パスワード: " + rawPassword + "\n" +
		//						"----------------------------------------\n\n" +
		//						"URL:○○○○〇\n\n" +
		//						"※パスワードの変更が完了するまで初期パスワードは紛失されないよう、お願いいたします。"
		//
		//		);
		// mailSender.send(message);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ】代表ユーザー初期アカウント発行");
		System.out.println("送信先: " + toEmail);
		System.out.println("初期パスワード: " + rawPassword);
		System.out.println("件名: 【重要】○○システムアカウント発行と初期パスワードのお知らせ");
		System.out.println("==================================================");
	}

	// パスワード再設定リンク送信

	@Override
	public void sendPasswordResetLinkEmail(String toEmail, String resetUrl) {
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(toEmail);
		//		message.setSubject("【重要】パスワード再設定のご案内");
		//		message.setText(
		//				"パスワード再設定のリクエストを受け付けました。\n\n" +
		//						"以下のリンクから新しいパスワードを設定してください（有効期限：1時間）。\n\n" +
		//						resetUrl + "\n\n" +
		//						"心当たりが無い場合は、このメールを破棄してください。");
		// mailSender.send(message);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ】パスワード再設定のご案内");
		System.out.println("送信先: " + toEmail);
		System.out.println("再設定URL: " + resetUrl);
		System.out.println("==================================================");
	}

	// メールアドレス変更通知
	@Override
	public void sendEmailChangedNotification(String oldEmail, String newEmail) {
		// 旧アドレス宛（乗っ取り検知用)
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(oldEmail); // ★あえて旧アドレス宛て（乗っ取り検知のため）
		//		message.setSubject("【重要】○○システムログインメールアドレス変更のお知らせ");
		//		message.setText(
		//				"このメールアドレスに登録されていたアカウントのログインメールアドレスが\n" +
		//						newEmail + " に変更されました。\n\n" +
		//						"心当たりが無い場合は、至急管理者へご連絡ください。");
		// mailSender.send(message);

		// 2. 新アドレス宛（変更完了の確認用）
		//        SimpleMailMessage newMessage = new SimpleMailMessage();
		//        newMessage.setTo(newEmail);
		//        newMessage.setSubject("【重要】○○システム 登録メールアドレス変更完了のお知らせ");
		//        newMessage.setText(
		//            "○○システムをご利用いただきありがとうございます。\n\n" +
		//            "ログイン用メールアドレスの変更が正常に完了いたしました。\n\n" +
		//            "今後は新しいメールアドレス（ " + newEmail + " ）にてログインを行ってください。"
		//        );
		// mailSender.send(newMessage);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ 1/2】旧アドレス宛（乗っ取り検知用）");
		System.out.println("送信先: " + oldEmail);
		System.out.println("件名: 【重要】○○システム ログインメールアドレス変更のお知らせ");
		System.out.println("本文: あなたのアカウントのログインメールアドレスが " + newEmail + " に変更されました。");
		System.out.println("--------------------------------------------------");
		System.out.println("【メール送信ログ 2/2】新アドレス宛（変更完了通知用）");
		System.out.println("送信先: " + newEmail);
		System.out.println("件名: 【重要】○○システム 登録メールアドレス変更完了のお知らせ");
		System.out.println("本文: ログイン用メールアドレスの変更が正常に完了いたしました。今後は " + newEmail + " でログインしてください。");
		System.out.println("==================================================");
	}

	// 代表ユーザー権限移譲通知
	@Override
	public void sendRoleTransferNotification(String newMasterEmail, String oldMasterEmail) {
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(newMasterEmail);
		//		message.setSubject("【重要】○○システム代表ユーザーへの変更のお知らせ");
		//		message.setText(
		//				"あなたが本システムの代表ユーザーに設定されました。\n" +
		//						"今後、『新規ユーザー追加』やユーザーの『無効/有効切り替え』が可能になります。");
		//		// mailSender.send(message);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ】代表ユーザーへの変更のお知らせ");
		System.out.println("新代表送信先: " + newMasterEmail);
		System.out.println("旧代表アドレス: " + oldMasterEmail);
		System.out.println("==================================================");
	}

	// 一般ユーザー追加
	@Override
	public void sendGeneralUserWelcomeEmail(String toEmail, String rawPassword) {
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(toEmail);
		//		message.setSubject("【重要】○○システムアカウント発行のお知らせ");
		//		message.setText(
		//				"○○システムへ、一般ユーザーアカウントの登録が完了いたしました。\n\n" +
		//						"以下のログイン情報でシステムへログインし、パスワードの変更をお願いいたします。\n\n" +
		//						"----------------------------------------\n" +
		//						"ログインID (メールアドレス): " + toEmail + "\n" +
		//						"初期パスワード: " + rawPassword + "\n" +
		//						"----------------------------------------\n\n" +
		//						"※パスワードの変更が完了するまで初期パスワードは紛失されないよう、お願いいたします。");
		// mailSender.send(message);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ】一般ユーザーアカウント発行");
		System.out.println("送信先: " + toEmail);
		System.out.println("初期パスワード: " + rawPassword);
		System.out.println("==================================================");
	}

	// 本人によるパスワード変更完了通知（旧メールアドレス宛）
	@Override
	public void sendPasswordChangedNotification(String toEmail) {
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(toEmail);
		//		message.setSubject("【重要】○○システム パスワード変更完了のお知らせ");
		//		message.setText(
		//				"○○システムをご利用いただきありがとうございます。\n\n" +
		//						"アカウントのパスワード変更が正常に完了いたしました。\n\n" +
		//						"※本通知に心当たりがない場合は、第三者による不正アクセスの可能性がございます。\n" +
		//						"至急、システム管理者または代表ユーザーまでご連絡ください。");
		// mailSender.send(message);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ】パスワード変更完了のお知らせ");
		System.out.println("送信先: " + toEmail);
		System.out.println("==================================================");
	}

	// 緊急・追加作業の受注のお知らせ（業者の代表ユーザー宛）
	// 請求の向きを矢印で書く：当社→御社（当社請求額）／御社→顧客（当社請求額 ×（1＋加算割合））
	@Override
	public void sendExtraWorkOrderedNotification(String toEmail, Project project, ExtraWork work,
			BigDecimal presentedSubtotal, BigDecimal tax) {
		BigDecimal ourSubtotal = work.getSubtotal() == null ? BigDecimal.ZERO : work.getSubtotal();
		BigDecimal ourTax = ourSubtotal.multiply(work.getTaxRate())
				.divide(new BigDecimal("100"), 0, RoundingMode.DOWN); // 税は切り捨て
		String ourAmounts = "当社→御社 請求額（税別）: " + yen(ourSubtotal)
				+ "／消費税: " + yen(ourTax)
				+ "／合計（税込）: " + yen(ourSubtotal.add(ourTax));
		String presentedAmounts = "御社→顧客 請求額（税別）: " + yen(presentedSubtotal)
				+ "／消費税: " + yen(tax)
				+ "／合計（税込）: " + yen(presentedSubtotal.add(tax));

		String rate = "御社設定加算割合: " + work.getMarkupRate().stripTrailingZeros().toPlainString() + "%";
		//		SimpleMailMessage message = new SimpleMailMessage();
		//		message.setTo(toEmail);
		//		message.setSubject("【○○システム】緊急・追加作業を受注しました（" + project.getProjectName() + "）");
		//		message.setText(
		//				"以下の緊急・追加作業を受注しました。\n\n" +
		//						"案件: " + project.getProjectName() + "\n" +
		//						"区分: " + work.getWorkType() + "\n" +
		//						"実施日: " + work.getWorkDate() + "\n" +
		//						(work.getLocation() == null ? "" : "場所: " + work.getLocation() + "\n") +
		//						"依頼主: " + work.getRequester() + "\n" +
		//						"依頼内容: " + work.getContent() + "\n\n" +
		//						ourAmounts + "\n" +
		//						presentedAmounts + "\n" +
		//						rate + "\n\n" +
		//						"毎次明細の発行時に請求に含めます。");
		// mailSender.send(message);
		System.out.println("==================================================");
		System.out.println("【メール送信ログ】緊急・追加作業の受注のお知らせ");
		System.out.println("送信先: " + toEmail);
		System.out.println("件名: 【○○システム】緊急・追加作業を受注しました（" + project.getProjectName() + "）");
		System.out.println("案件: " + project.getProjectName() + "（" + project.getClientName() + "）");
		System.out.println("区分: " + work.getWorkType() + "／実施日: " + work.getWorkDate()
				+ (work.getLocation() == null ? "" : "／場所: " + work.getLocation()));
		System.out.println("依頼主: " + work.getRequester());
		System.out.println("依頼内容: " + work.getContent());
		System.out.println(ourAmounts);
		System.out.println(presentedAmounts);
		System.out.println(rate);
		System.out.println("受注: " + work.getOrderedAt() + "（" + work.getOrderedBy() + "）");
		System.out.println("==================================================");
	}

	// 金額を「12,345円」の形にする
	private String yen(BigDecimal amount) {
		return String.format("%,d", amount.longValue()) + "円";
	}


}