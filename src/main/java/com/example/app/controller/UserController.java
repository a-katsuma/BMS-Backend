package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.User;
import com.example.app.service.BusinessPolicyService;
import com.example.app.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users") // あるいは /api にしてもOKです
@RequiredArgsConstructor
public class UserController {

	private final PasswordEncoder passwordEncoder;
	private final UserService userService;
	private final BusinessPolicyService businessPolicyService;

	private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern
			.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	//ログイン処理
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody User loginRequest, HttpSession session) {
		// user_id ではなく login_id で検索する
		User user = userService.getUserByLoginId(loginRequest.getLoginId());

		// 1. ユーザーが存在しない、またはパスワードが一致しない場合
		if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body("ログインIDまたはパスワードが正しくありません");
		}

		// 2. アカウントが無効化されている場合 (0 の場合)
		if (user.getIsActive() != null && user.getIsActive() == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("このアカウントは無効化されています。管理者にお問い合わせください。");
		}

		// 3. 認証成功処理
		session.setAttribute("LOGIN_USER", user);
		return ResponseEntity.ok(user);
	}

	// ログアウト処理
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.ok("ログアウトしました");
	}

	// 現在のログインユーザー情報取得
	@GetMapping("/current")
	public ResponseEntity<?> getCurrentUser(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログイン");
		}

		Map<String, Object> response = new HashMap<>();
		response.put("user", user);

		// ★ 命名統一：businessPolicyAgreed として返却
		if (user.getCompanyId() != null) {
			boolean businessPolicyAgreed = businessPolicyService.isCurrentlyAgreed(user.getCompanyId());
			response.put("businessPolicyAgreed", businessPolicyAgreed);
			// 未選択（レコードなし or 譲渡リセット）かどうか
			response.put("businessPolicySet", businessPolicyService.isPolicySet(user.getCompanyId()));
		} else {
			response.put("businessPolicyAgreed", true);
			response.put("businessPolicySet", true);
		}

		return ResponseEntity.ok(response);
	}

	
	@GetMapping("/me/company/business-policy")
	public ResponseEntity<?> getMyBusinessPolicy(HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("errorMessage", "未ログインです"));
		}

		if (currentUser.getCompanyId() == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorMessage", "所属会社情報が存在しません"));
		}

		return ResponseEntity.ok(businessPolicyService.getCurrentPolicy(currentUser.getCompanyId()));
	}

	@PostMapping("/me/company/business-policy")
	public ResponseEntity<?> setMyBusinessPolicy(
			@RequestBody Map<String, Object> body,
			HttpSession session) {

		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("errorMessage", "未ログインです"));
		}

		if (currentUser.getRoleFlag() != 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("errorMessage", "代表ユーザーのみ設定可能です"));
		}

		try {
			// 1. リクエストボディから値を取り出す
			boolean isAgreed = Boolean.TRUE.equals(body.get("isAgreed"));

			// markupRate (BigDecimalへの安全な変換)
			java.math.BigDecimal markupRate = null;
			if (body.get("markupRate") != null) {
				markupRate = new java.math.BigDecimal(body.get("markupRate").toString());
			}

			boolean acknowledge = Boolean.TRUE.equals(body.get("acknowledgePendingWorkUsesOldRate"));

			// 2. 既存設定があるか確認し、setReason (理由) を判定する
			boolean isAlreadySet = businessPolicyService.isPolicySet(currentUser.getCompanyId());
			String setReason = isAlreadySet ? "CHANGE" : "INITIAL";

			// 3. updatePolicy ではなく setPolicy を正しい引数で呼び出す
			businessPolicyService.setPolicy(
					currentUser.getCompanyId(),
					isAgreed,
					markupRate,
					currentUser.getName(), // setBy (操作者名)
					setReason, // setReason (INITIAL または CHANGE)
					acknowledge // acknowledgePendingWorkUsesOldRate
			);

			return ResponseEntity.ok(Map.of("message", "ビジネスポリシーを更新しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("errorMessage", "設定の更新に失敗しました。"));
		}
	}

	// 管理者が対象ユーザーのパスワード再発行を要求
	@PostMapping("/{userId}/reset-password")
	public ResponseEntity<?> requestPasswordResetByAdmin(@PathVariable Integer userId, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");

		// 管理者(roleFlag == 1)のみ許可
		if (currentUser == null || currentUser.getRoleFlag() != 1) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "管理者のみ実行できます"));
		}

		try {
			userService.requestPasswordReset(userId, currentUser.getName());
			return ResponseEntity.ok(Map.of("message", "パスワード再設定用のメールを送信しました。"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// ユーザーの有効/無効ステータス切り替えAPI
	@PatchMapping("/{userId}/status")
	public ResponseEntity<?> toggleUserStatus(@PathVariable Integer userId, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}

		User target = userService.getUserById(userId);
		if (target == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorMessage", "対象ユーザーが見つかりません"));
		}

		boolean isAdmin = currentUser.getRoleFlag() == 1;
		boolean isSameCompanyRepresentative = currentUser.getRoleFlag() == 2
				&& currentUser.getCompanyId() != null
				&& currentUser.getCompanyId().equals(target.getCompanyId());

		if (!isAdmin && !isSameCompanyRepresentative) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "権限がありません"));
		}
		// 代表ユーザーは、他の代表（自分自身含む）を操作できない
		if (isSameCompanyRepresentative && target.getRoleFlag() != 3) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "一般ユーザーのみ操作できます"));
		}

		userService.toggleStatus(userId);
		return ResponseEntity.ok(Map.of("message", "ステータスを変更しました。"));
	}

	// 本人がログイン後にパスワード変更
	@PostMapping("/me/password")
	public ResponseEntity<?> changeMyPassword(@RequestBody Map<String, String> body, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}
		try {
			userService.changeOwnPassword(currentUser.getUserId(), body.get("currentPassword"),
					body.get("newPassword"));
			return ResponseEntity.ok(Map.of("message", "パスワードを変更しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// 本人がログイン後にメールアドレス変更
	@PatchMapping("/me/email")
	public ResponseEntity<?> updateMyEmail(@RequestBody Map<String, String> body, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}
		try {
			userService.updateOwnEmail(currentUser.getUserId(), body.get("currentPassword"), body.get("newEmail"));
			return ResponseEntity.ok(Map.of("message", "メールアドレスを変更しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// メールアドレス変更
	@PatchMapping("/{userId}/email")
	public ResponseEntity<?> updateEmail(@PathVariable Integer userId,
			@RequestBody Map<String, String> body, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null || currentUser.getRoleFlag() != 1) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "管理者のみ実行できます"));
		}
		try {
			userService.updateMasterEmail(userId, body.get("email"), currentUser.getName());
			return ResponseEntity.ok(Map.of("message", "メールアドレスを更新しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// ユーザー名変更（本人）
	@PatchMapping("/me/name")
	public ResponseEntity<?> updateMyName(@RequestBody Map<String, String> body, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}
		try {
			userService.updateOwnName(currentUser.getUserId(), body.get("name"));
			return ResponseEntity.ok(Map.of("message", "氏名を変更しました。"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// 代表ユーザーの交代
	@PatchMapping("/companys/{companyId}/master/transfer")
	public ResponseEntity<?> transferMaster(@PathVariable Integer companyId,
			@RequestBody Map<String, Integer> body, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null || currentUser.getRoleFlag() != 1) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "管理者のみ実行できます"));
		}
		try {
			userService.transferMasterRole(companyId, body.get("newMasterUserId"), currentUser.getName());
			return ResponseEntity.ok(Map.of("message", "代表ユーザーを交代しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// 代表ユーザーが自社の一般ユーザーを追加
	@PostMapping("/me/company/users")
	public ResponseEntity<?> addGeneralUser(@RequestBody Map<String, String> body, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}
		if (currentUser.getRoleFlag() != 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "代表ユーザーのみ実行できます"));
		}

		String name = body.get("name");
		String email = body.get("email");
		if (name == null || name.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", "氏名を入力してください"));
		}
		if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", "メールアドレスの形式が正しくありません"));
		}

		try {
			userService.addGeneralUser(currentUser.getCompanyId(), name, email, currentUser.getName());
			return ResponseEntity.ok(Map.of("message", "一般ユーザーを追加し、招待メールを送信しました。"));
		} catch (IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// 代表ユーザーが自社のユーザー一覧を取得
	@GetMapping("/me/company/users")
	public ResponseEntity<?> getMyCompanyUsers(HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}
		if (currentUser.getRoleFlag() != 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "代表ユーザーのみ実行できます"));
		}
		return ResponseEntity.ok(Map.of("users", userService.getUsersByCompanyId(currentUser.getCompanyId())));
	}

	// 代表ユーザーによる自社一般ユーザーのメールアドレス変更
	@PatchMapping("/me/company/users/{userId}/email")
	public ResponseEntity<?> updateGeneralUserEmail(
			@PathVariable Integer userId,
			@RequestBody Map<String, String> body,
			HttpSession session) {

		User currentUser = (User) session.getAttribute("LOGIN_USER");

		// 1. ログインチェックおよび代表ユーザー（roleFlag == 2）権限チェック
		if (currentUser == null || currentUser.getRoleFlag() != 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("errorMessage", "代表ユーザーのみ実行可能です。"));
		}

		// 2. 変更対象ユーザーの存在チェックと自社所属・一般ユーザー判定
		User targetUser = userService.getUserById(userId);
		if (targetUser == null
				|| !currentUser.getCompanyId().equals(targetUser.getCompanyId())
				|| targetUser.getRoleFlag() != 3) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("errorMessage", "自社の一般ユーザーのみ変更可能です。"));
		}

		String newEmail = body.get("email");
		if (newEmail == null || newEmail.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", "新しいメールアドレスを入力してください。"));
		}

		try {
			// 既存の管理者代理用メソッド（履歴記録・旧アドレスへの通知メール送信を含む）を呼び出し
			userService.updateMasterEmail(userId, newEmail, currentUser.getName());
			return ResponseEntity.ok(Map.of("message", "メールアドレスを変更しました。"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// 代表ユーザーが自社の一般ユーザーのパスワード再発行を要求
	@PostMapping("/me/company/users/{userId}/reset-password")
	public ResponseEntity<?> resetGeneralUserPassword(@PathVariable Integer userId, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorMessage", "未ログインです"));
		}
		if (currentUser.getRoleFlag() != 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "代表ユーザーのみ実行できます"));
		}

		User target = userService.getUserById(userId);
		if (target == null || !currentUser.getCompanyId().equals(target.getCompanyId()) || target.getRoleFlag() != 3) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "自社の一般ユーザーのみ対象にできます"));
		}

		userService.requestPasswordReset(userId, currentUser.getName());
		return ResponseEntity.ok(Map.of("message", "パスワード再設定用のメールを送信しました。"));
	}

		// パスワードを忘れた場合（ログイン画面から・未ログイン）
	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
		String loginId = body.get("loginId");
		if (loginId == null || loginId.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", "ログインIDを入力してください。"));
		}

		// 存在しないIDでも同じ応答を返す（アカウントの有無を外部に知られないため）
		userService.requestPasswordResetByLoginId(loginId.trim());
		return ResponseEntity.ok(Map.of("message", "登録されているメールアドレスに再設定用のメールを送信しました。"));
	}



	// トークンによるパスワード再設定の確定（メールリンクからの変更処理）
	@PostMapping("/reset-password/confirm")
	public ResponseEntity<?> confirmPasswordReset(@RequestBody Map<String, String> body) {
		String token = body.get("token");
		String newPassword = body.get("newPassword");

		if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", "トークンまたは新しいパスワードが正しくありません。"));
		}

		try {
			// UserServiceの confirmPasswordReset を呼び出す
			userService.confirmPasswordReset(token, newPassword);
			return ResponseEntity.ok(Map.of("message", "パスワードの再設定が完了しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}
}