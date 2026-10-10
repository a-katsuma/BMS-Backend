package com.example.app.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.app.domain.User;

/**
 * API の入口で、ログインと立場（roleFlag）を共通で確認する
 *   /api/contractee/** ：業者（代表 2・一般 3）だけ
 *   /api/users/**      ：ログインしていれば通す（立場は UserController の中で個別に確認する）
 *   それ以外の /api/** ：管理者（1）だけ
 * ログインなしで使う API（ログイン・パスワード再設定・デモ）は、ApplicationConfig で対象から外す
 */
public class AuthInterceptor implements HandlerInterceptor {

	private static final int ROLE_ADMIN = 1;
	private static final int ROLE_MASTER = 2;
	private static final int ROLE_GENERAL = 3;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		// CORS の事前確認（開発環境の localhost:3000 → 8080）はログイン情報を持たないので通す
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		// セッションがなければ作らない（getSession(false)）
		HttpSession session = request.getSession(false);
		User user = (session == null) ? null : (User) session.getAttribute("LOGIN_USER");
		if (user == null || user.getRoleFlag() == null) {
			return reject(response, HttpStatus.UNAUTHORIZED, "ログインしてください。");
		}

		int role = user.getRoleFlag();
		String path = request.getRequestURI();

		// 業者用の API
		if (path.startsWith("/api/contractee/")) {
			if (role == ROLE_MASTER || role == ROLE_GENERAL) {
				return true;
			}
			return reject(response, HttpStatus.FORBIDDEN, "発注元のユーザーだけが使えます。");
		}

		// ユーザーの API（自分の情報の変更など。立場はコントローラーの中で確認する）
		if (path.startsWith("/api/users/")) {
			return true;
		}

		// それ以外は管理者用の API
		if (role == ROLE_ADMIN) {
			return true;
		}
		return reject(response, HttpStatus.FORBIDDEN, "管理者だけが使えます。");
	}

	// エラーを { "errorMessage": "..." } の形で返して、コントローラーには進ませない
	private boolean reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"errorMessage\":\"" + message + "\"}");
		return false;
	}
}
