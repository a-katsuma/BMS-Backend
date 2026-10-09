package com.example.app.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.app.domain.User;

// デモモードの設定（application.properties の app.demo.*）
//   enabled=true のときだけ、パスワードなしのデモ用ログイン（DemoController）が使える
@Component
public class DemoAccounts {

	public static final String ROLE_ADMIN = "ADMIN";     // 管理者
	public static final String ROLE_MASTER = "MASTER";   // 業者の代表
	public static final String ROLE_GENERAL = "GENERAL"; // 業者の一般

	private final boolean enabled;
	private final Map<String, String> loginIds;

	public DemoAccounts(
			@Value("${app.demo.enabled}") boolean enabled,
			@Value("${app.demo.admin-login-id}") String adminLoginId,
			@Value("${app.demo.master-login-id}") String masterLoginId,
			@Value("${app.demo.general-login-id}") String generalLoginId) {
		this.enabled = enabled;
		this.loginIds = Map.of(
				ROLE_ADMIN, adminLoginId,
				ROLE_MASTER, masterLoginId,
				ROLE_GENERAL, generalLoginId);
	}

	public boolean isEnabled() {
		return enabled;
	}

	// 種類（ADMIN／MASTER／GENERAL）→ ログインID。デモモードでない・種類が違うときは null
	public String loginIdOf(String role) {
		return (enabled && role != null) ? loginIds.get(role) : null;
	}

	// デモ用ログインで使うアカウントか（デモモードのときだけ true になる）
	public boolean isDemoAccount(User user) {
		return enabled && user != null && loginIds.containsValue(user.getLoginId());
	}
}
