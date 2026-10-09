package com.example.app.controller;

import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.config.DemoAccounts;
import com.example.app.domain.User;
import com.example.app.service.UserService;

import lombok.RequiredArgsConstructor;

// 公開デモ用（app.demo.enabled=true のときだけ動く）
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/demo")
public class DemoController {

	private final DemoAccounts demoAccounts;
	private final UserService userService;

	// ① デモモードかどうか（ログイン画面に、デモ用ログインのボタンを出すか）
	@GetMapping
	public Map<String, Object> info() {
		return Map.of("enabled", demoAccounts.isEnabled());
	}

	// ② デモ用ログイン（パスワードなし。body：{ role: "ADMIN" / "MASTER" / "GENERAL" }）
	//    デモモードでなければ、この API は無いものとして 404 を返す
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
		if (!demoAccounts.isEnabled()) {
			return ResponseEntity.notFound().build();
		}
		String loginId = demoAccounts.loginIdOf(body.get("role"));
		if (loginId == null) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", "ログインする種類が正しくありません。"));
		}
		User user = userService.getUserByLoginId(loginId);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("errorMessage", "デモ用のアカウントが見つかりません。"));
		}
		if (user.getIsActive() != null && user.getIsActive() == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("errorMessage", "このデモ用のアカウントは無効になっています。"));
		}
		session.setAttribute("LOGIN_USER", user);
		return ResponseEntity.ok(user);
	}
}
