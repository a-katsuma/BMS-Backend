package com.example.app.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.User;
import com.example.app.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users") // あるいは /api にしてもOKです
@RequiredArgsConstructor
public class UserController {

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final UserService userService;

	//ログイン処理
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody User loginRequest, HttpSession session) {
		// user_id ではなく login_id で検索する
		User user = userService.getUserByLoginId(loginRequest.getLoginId());

		// ユーザーが存在し、かつ入力されたパスワードがDBのBCryptハッシュと一致するかをチェック
		if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
			session.setAttribute("LOGIN_USER", user);
			return ResponseEntity.ok(user);
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインに失敗しました");
		}
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
		if (user != null) {
			return ResponseEntity.ok(user);
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログイン");
		}
	}
}