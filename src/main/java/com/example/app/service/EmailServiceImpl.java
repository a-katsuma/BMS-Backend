package com.example.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;

import lombok.extern.slf4j.Slf4j;

// メールの送信
//   app.mail.enabled=false（開発の初期値）：送らずに、内容をコンソールに表示するだけ
//   app.mail.enabled=true （Docker）       ：実際に送る（デモ環境では Mailpit が受け止める）
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

	private static final String SYSTEM_NAME = "BMS System";

	private final JavaMailSender mailSender;
	private final boolean mailEnabled;
	private final String from;
	private final String frontendBaseUrl;

	public EmailServiceImpl(JavaMailSender mailSender,
			@Value("${app.mail.enabled}") boolean mailEnabled,
			@Value("${app.mail.from}") String from,
			@Value("${app.frontend-base-url}") String frontendBaseUrl) {
		this.mailSender = mailSender;
		this.mailEnabled = mailEnabled;
		this.from = from;
		this.frontendBaseUrl = frontendBaseUrl;
	}

		// アカウント発行時の招待：パスワード設定のリンク（業者の代表・一般ユーザー共通）
	@Override
	public void sendInvitationEmail(String toEmail, String roleLabel, String setupUrl, int validHours) {
		send(toEmail, "【重要】" + SYSTEM_NAME + " アカウント発行とパスワード設定のご案内",
				SYSTEM_NAME + "へ、" + roleLabel + "のアカウントを登録しました。\n\n"
						+ "以下のリンクからパスワードを設定してください（有効期限：" + validHours + "時間）。\n\n"
						+ setupUrl + "\n\n"
						+ "----------------------------------------\n"
						+ "ログインID（メールアドレス）: " + toEmail + "\n"
						+ "ログイン画面: " + frontendBaseUrl + "/login\n"
						+ "----------------------------------------\n\n"
						+ "※有効期限が切れた場合は、ログイン画面の「パスワードをお忘れですか？」から、もう一度設定できます。");
	}


	// パスワード再設定リンクの送信
	@Override
	public void sendPasswordResetLinkEmail(String toEmail, String resetUrl) {
		send(toEmail, "【重要】" + SYSTEM_NAME + " パスワード再設定のご案内",
				"パスワード再設定のリクエストを受け付けました。\n\n"
						+ "以下のリンクから新しいパスワードを設定してください（有効期限：1時間）。\n\n"
						+ resetUrl + "\n\n"
						+ "心当たりが無い場合は、このメールを破棄してください。");
	}

	// メールアドレス変更の通知（旧アドレス宛：乗っ取り検知用／新アドレス宛：変更完了の確認用）
	@Override
	public void sendEmailChangedNotification(String oldEmail, String newEmail) {
		send(oldEmail, "【重要】" + SYSTEM_NAME + " ログインメールアドレス変更のお知らせ",
				"このメールアドレスに登録されていたアカウントのログインメールアドレスが\n"
						+ newEmail + " に変更されました。\n\n"
						+ "心当たりが無い場合は、至急管理者へご連絡ください。");

		send(newEmail, "【重要】" + SYSTEM_NAME + " 登録メールアドレス変更完了のお知らせ",
				SYSTEM_NAME + "をご利用いただきありがとうございます。\n\n"
						+ "ログイン用メールアドレスの変更が正常に完了いたしました。\n\n"
						+ "今後は新しいメールアドレス（ " + newEmail + " ）にてログインを行ってください。");
	}

	// 代表ユーザーの交代の通知（新しい代表宛）
	@Override
	public void sendRoleTransferNotification(String newMasterEmail, String oldMasterEmail) {
		send(newMasterEmail, "【重要】" + SYSTEM_NAME + " 代表ユーザーへの変更のお知らせ",
				"あなたが本システムの代表ユーザーに設定されました（前の代表：" + oldMasterEmail + "）。\n"
						+ "今後、『新規ユーザー追加』やユーザーの『無効/有効切り替え』が可能になります。");
	}


	// 本人によるパスワード変更の完了通知
	@Override
	public void sendPasswordChangedNotification(String toEmail) {
		send(toEmail, "【重要】" + SYSTEM_NAME + " パスワード変更完了のお知らせ",
				SYSTEM_NAME + "をご利用いただきありがとうございます。\n\n"
						+ "アカウントのパスワード変更が正常に完了いたしました。\n\n"
						+ "※本通知に心当たりがない場合は、第三者による不正アクセスの可能性がございます。\n"
						+ "至急、システム管理者または代表ユーザーまでご連絡ください。");
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

		send(toEmail, "【" + SYSTEM_NAME + "】緊急・追加作業を受注しました（" + project.getProjectName() + "）",
				"以下の緊急・追加作業を受注しました。\n\n"
						+ "案件: " + project.getProjectName() + "（" + project.getClientName() + "）\n"
						+ "区分: " + work.getWorkType() + "\n"
						+ "実施日: " + work.getWorkDate() + "\n"
						+ (work.getLocation() == null ? "" : "場所: " + work.getLocation() + "\n")
						+ "依頼主: " + work.getRequester() + "\n"
						+ "依頼内容: " + work.getContent() + "\n\n"
						+ ourAmounts + "\n"
						+ presentedAmounts + "\n"
						+ rate + "\n\n"
						+ "毎次明細の発行時に請求に含めます。");
	}

	// ---------- 共通 ----------

	// 送信する。無効の設定なら送らずに、内容をコンソールに表示する（開発で中身を確認できるように）
	// 送信に失敗しても、元の処理（業者の登録など）は止めない（ログに残すだけ）
	private void send(String to, String subject, String text) {
		if (!mailEnabled) {
			log.info("\n==================================================\n"
					+ "【メール（送信しない設定）】\n宛先: {}\n件名: {}\n\n{}\n"
					+ "==================================================", to, subject, text);
			return;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(text);
			mailSender.send(message);
			log.info("メールを送信しました（宛先: {}／件名: {}）", to, subject);
		} catch (MailException e) {
			log.error("メールを送信できませんでした（宛先: {}／件名: {}）", to, subject, e);
		}
	}

	// 金額を「12,345円」の形にする
	private String yen(BigDecimal amount) {
		return String.format("%,d", amount.longValue()) + "円";
	}
}
