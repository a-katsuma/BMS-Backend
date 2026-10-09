package com.example.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.PasswordResetToken;
import com.example.app.domain.User;
import com.example.app.domain.UserChangeHistory;
import com.example.app.mapper.PasswordResetTokenMapper;
import com.example.app.mapper.UserChangeHistoryMapper;
import com.example.app.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	// パスワード設定のリンクの有効期限（時間）
	private static final int RESET_VALID_HOURS = 1;       // パスワード再設定
	private static final int INVITATION_VALID_HOURS = 72; // アカウント発行時の招待（すぐにメールを見るとは限らないため長め）

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final UserChangeHistoryMapper userChangeHistoryMapper;
	private final PasswordResetTokenMapper passwordResetTokenMapper;
	private final BusinessPolicyService businessPolicyService;

	@Value("${app.frontend-base-url}")
	private String frontendBaseUrl;

	@Override
	public User getUserById(Integer id) {
		return userMapper.selectById(id);
	}

	@Override
	public User getUserByLoginId(String loginId) {
		return userMapper.selectByLoginId(loginId);
	}

	// 業者IDに紐づくユーザー一覧を取得
	@Override
	public List<User> getUsersByCompanyId(Integer companyId) {
		return userMapper.selectByCompanyId(companyId);
	}

	// ステータス（1:有効 / 0:無効）を指定して更新（同じ値を何度送っても結果は同じ）
	@Override
	public void updateStatus(Integer userId, int isActive) {
		if (isActive != 0 && isActive != 1) {
			throw new IllegalArgumentException("ステータスの値が正しくありません。");
		}
		userMapper.updateStatus(userId, isActive);
	}

	// ---------- パスワード設定のリンク（再設定・招待で共通） ----------

	// トークンを発行して、パスワード設定の画面の URL を返す（前に発行したトークンは無効にする）
	private String issueToken(User user, int validHours) {
		passwordResetTokenMapper.invalidateAllForUser(user.getUserId());

		String token = UUID.randomUUID().toString().replace("-", "");
		passwordResetTokenMapper.insert(PasswordResetToken.builder()
				.userId(user.getUserId())
				.token(token)
				.expiresAt(LocalDateTime.now().plusHours(validHours))
				.build());

		return frontendBaseUrl + "/reset-password?token=" + token;
	}

	// パスワード再設定のリンクを送る（有効期限 1時間）
	private void issuePasswordResetToken(User user) {
		String resetUrl = issueToken(user, RESET_VALID_HOURS);
		emailService.sendPasswordResetLinkEmail(user.getEmail(), resetUrl);
	}

	// アカウント発行時の招待：パスワード設定のリンクを送る（有効期限 72時間）
	@Override
	public void sendInvitation(User user) {
		String setupUrl = issueToken(user, INVITATION_VALID_HOURS);
		String roleLabel = Integer.valueOf(2).equals(user.getRoleFlag()) ? "代表ユーザー"
				: Integer.valueOf(3).equals(user.getRoleFlag()) ? "一般ユーザー"
				: "ユーザー";
		emailService.sendInvitationEmail(user.getEmail(), roleLabel, setupUrl, INVITATION_VALID_HOURS);
	}

	// ①管理者が代理で要求
	@Override
	public void requestPasswordReset(Integer userId, String operatorName) {
		User user = userMapper.selectById(userId);
		if (user == null) {
			throw new IllegalArgumentException("対象ユーザーが見つかりません");
		}
		issuePasswordResetToken(user);

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(userId).companyId(user.getCompanyId())
				.changeType("PASSWORD_RESET_REQUESTED")
				.operatedBy(operatorName).build());
	}

	// ①’本人が「パスワードを忘れた」から要求
	@Override
	public void requestPasswordResetByLoginId(String loginId) {
		User user = userMapper.selectByLoginId(loginId);
		// ★存在しない場合やアカウント無効時も、ここでは例外を投げない
		//   （メールアドレスの存在有無が外部から分かってしまう「列挙攻撃」を防ぐため）
		if (user != null && user.getIsActive() == 1) {
			issuePasswordResetToken(user);
			userChangeHistoryMapper.insert(UserChangeHistory.builder()
					.userId(user.getUserId()).companyId(user.getCompanyId())
					.changeType("PASSWORD_RESET_REQUESTED")
					.operatedBy("本人（パスワード紛失）").build());
		}
	}

	// トークンから新パスワードを確定（再設定・招待で共通）
	@Override
	public void confirmPasswordReset(String token, String newPassword) {
		PasswordResetToken prt = passwordResetTokenMapper.selectByToken(token);
		if (prt == null || prt.getUsed() == 1 || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalStateException("このリンクは無効か、有効期限が切れています");
		}

		userMapper.updatePassword(prt.getUserId(), passwordEncoder.encode(newPassword));
		passwordResetTokenMapper.markUsed(token);

		User user = userMapper.selectById(prt.getUserId());
		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(prt.getUserId()).companyId(user.getCompanyId())
				.changeType("PASSWORD_RESET_COMPLETED")
				.operatedBy("本人").build());
	}

	// 本人がログイン後に直接変更
	@Override
	public void changeOwnPassword(Integer userId, String currentPassword, String newPassword) {
		User user = userMapper.selectById(userId);
		if (user == null) {
			throw new IllegalArgumentException("ユーザーが見つかりません");
		}
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new IllegalStateException("現在のパスワードが正しくありません");
		}
		userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));

		// ★追加：本人の現在のアドレスへパスワード変更完了メールを送信
		emailService.sendPasswordChangedNotification(user.getEmail());

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(userId).companyId(user.getCompanyId())
				.changeType("PASSWORD_CHANGE")
				.operatedBy("本人").build());
	}

	// ---------- メールアドレス変更 ----------

	// ②管理者代理
	@Override
	public void updateMasterEmail(Integer userId, String newEmail, String operatorName) {
		User existing = userMapper.selectByLoginId(newEmail);
		if (existing != null && !existing.getUserId().equals(userId)) {
			throw new IllegalStateException("そのメールアドレスは既に使用されています");
		}

		User user = userMapper.selectById(userId);
		if (user == null) {
			throw new IllegalArgumentException("対象ユーザーが見つかりません");
		}
		String oldEmail = user.getEmail();

		userMapper.updateLoginIdAndEmail(userId, newEmail, newEmail);
		emailService.sendEmailChangedNotification(oldEmail, newEmail);

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(userId).companyId(user.getCompanyId())
				.changeType("EMAIL_CHANGE").oldValue(oldEmail).newValue(newEmail)
				.operatedBy(operatorName).build());
	}

	// ② 本人がログイン後
	@Override
	public void updateOwnEmail(Integer userId, String currentPassword, String newEmail) {
		User user = userMapper.selectById(userId);
		if (user == null) {
			throw new IllegalArgumentException("ユーザーが見つかりません");
		}
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new IllegalStateException("現在のパスワードが正しくありません");
		}

		User existing = userMapper.selectByLoginId(newEmail);
		if (existing != null && !existing.getUserId().equals(userId)) {
			throw new IllegalStateException("そのメールアドレスは既に使用されています");
		}

		String oldEmail = user.getEmail();
		userMapper.updateLoginIdAndEmail(userId, newEmail, newEmail);
		emailService.sendEmailChangedNotification(oldEmail, newEmail);

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(userId).companyId(user.getCompanyId())
				.changeType("EMAIL_CHANGE").oldValue(oldEmail).newValue(newEmail)
				.operatedBy("本人").build());
	}

	// ---------- 代表ユーザーの交代 ----------

	@Override
	public void transferMasterRole(Integer companyId, Integer newMasterUserId, String operatorName) {
		List<User> companyUsers = userMapper.selectByCompanyId(companyId);

		User currentMaster = companyUsers.stream()
				.filter(u -> u.getRoleFlag() == 2)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("現在の代表ユーザーが見つかりません"));

		User newMaster = companyUsers.stream()
				.filter(u -> u.getUserId().equals(newMasterUserId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("指定ユーザーが自社に所属していません"));

		if (newMaster.getRoleFlag() == 2) {
			throw new IllegalStateException("既に代表ユーザーです");
		}
		if (newMaster.getIsActive() == 0) {
			throw new IllegalStateException("無効化されたユーザーは代表にできません");
		}

		userMapper.updateRoleFlag(currentMaster.getUserId(), 3);
		userMapper.updateRoleFlag(newMaster.getUserId(), 2);

		businessPolicyService.resetPolicyOnTransfer(companyId, operatorName);

		emailService.sendRoleTransferNotification(newMaster.getEmail(), currentMaster.getEmail());

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(currentMaster.getUserId()).companyId(companyId)
				.changeType("ROLE_TRANSFER").oldValue("代表").newValue("一般")
				.operatedBy(operatorName).build());
		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(newMaster.getUserId()).companyId(companyId)
				.changeType("ROLE_TRANSFER").oldValue("一般").newValue("代表")
				.operatedBy(operatorName).build());
	}

	// ユーザー名変更（本人）
	@Override
	public void updateOwnName(Integer userId, String newName) {
		if (newName == null || newName.isBlank()) {
			throw new IllegalArgumentException("氏名を入力してください");
		}

		User user = userMapper.selectById(userId);
		if (user == null) {
			throw new IllegalArgumentException("ユーザーが見つかりません");
		}

		String oldName = user.getName();
		if (newName.equals(oldName)) {
			return; // 変更なしなら何もしない
		}

		userMapper.updateName(userId, newName);

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(userId).companyId(user.getCompanyId())
				.changeType("NAME_CHANGE")
				.oldValue(oldName).newValue(newName)
				.operatedBy("本人").build());
	}

	// 一般ユーザー追加（パスワードは、招待のリンクから本人が決める）
	@Override
	public User addGeneralUser(Integer companyId, String name, String email, String operatorName) {
		User existing = userMapper.selectByLoginId(email);
		if (existing != null) {
			throw new IllegalStateException("そのメールアドレスは既に使用されています");
		}

		User user = new User();
		user.setLoginId(email);
		// 本人がリンクから設定するまでは、誰も知らないランダムな値にしておく
		user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setName(name);
		user.setCompanyId(companyId);
		user.setRoleFlag(3); // 一般ユーザー
		user.setEmail(email);
		user.setIsActive(1);

		userMapper.insert(user); // useGeneratedKeysでuserIdが自動セットされる

		sendInvitation(user);

		userChangeHistoryMapper.insert(UserChangeHistory.builder()
				.userId(user.getUserId()).companyId(companyId)
				.changeType("USER_ADDED")
				.newValue(email)
				.operatedBy(operatorName).build());

		return user;
	}

}
