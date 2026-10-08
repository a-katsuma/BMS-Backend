package com.example.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingStatement;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.service.BillingBaseService;
import com.example.app.service.BillingStatementService;
import com.example.app.service.BusinessPolicyService;
import com.example.app.service.ExtraWorkService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/statements")
public class BillingStatementController {

	private final BillingStatementService statementService;
	private final BillingBaseService billingBaseService;
	private final ProjectService projectService;
	private final BusinessPolicyService businessPolicyService;
	private final ExtraWorkService extraWorkService;

	// ① 一覧（全件）＋作成時に選べるベース（使用中のみ）
	@GetMapping
	public ResponseEntity<?> list(@PathVariable Integer projectId, HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		List<BillingBase> bases = billingBaseService.getBases(projectId, true);

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("statements", statementService.getStatements(projectId, false));
		response.put("bases", bases);
		return ResponseEntity.ok(response);
	}

	// ② 1件
	@GetMapping("/{statementId}")
	public ResponseEntity<?> show(@PathVariable Integer projectId, @PathVariable Integer statementId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		BillingStatement statement = statementService.getStatement(projectId, statementId);
		if (project == null || statement == null) {
			return ResponseEntity.notFound().build();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("statement", statement);
		response.put("policies", businessPolicyService.getPolicyHistory(project.getCompanyId())); // 実施日時点の事前承認の判定用
		// 下書き：受注済みから取り込める緊急・追加作業（同じ顧客・業者で、受注済み・未請求・未取り込み）
		if (BillingStatement.STATUS_DRAFT.equals(statement.getStatus())) {
			response.put("importCandidates",
					extraWorkService.getImportCandidates(project.getClientId(), project.getCompanyId()));
		}
		return ResponseEntity.ok(response);
	}

	// ③ 作成（baseId は使用中のベースが1つだけなら省略可）
	@PostMapping("/add")
	public ResponseEntity<?> create(@PathVariable Integer projectId, @RequestBody BillingStatement form,
			HttpSession session) {
		User user = loginAdmin(session);
		if (user == null) {
			return forbidden();
		}
		try {
			Integer statementId = statementService.create(projectId, form.getBaseId(), form.getBillingMonth(),
					user.getName());
			return ResponseEntity.ok(Map.of("message", "毎次明細を作成しました。", "statementId", statementId));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return badRequest(e);
		}
	}

	// ④ 下書き保存
	@PutMapping("/{statementId}")
	public ResponseEntity<?> save(@PathVariable Integer projectId, @PathVariable Integer statementId,
			@RequestBody BillingStatement form, HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> statementService.save(projectId, statementId, form), "下書きを保存しました。");
	}

	// ⑤ 確定
	@PostMapping("/{statementId}/confirm")
	public ResponseEntity<?> confirm(@PathVariable Integer projectId, @PathVariable Integer statementId,
			@RequestBody BillingStatement form, HttpSession session) {
		User user = loginAdmin(session);
		if (user == null) {
			return forbidden();
		}
		return run(() -> statementService.confirm(projectId, statementId, form, user.getName()),
				"毎次明細を確定しました。");
	}

	// ⑥ 確定解除
	@PostMapping("/{statementId}/unconfirm")
	public ResponseEntity<?> unconfirm(@PathVariable Integer projectId, @PathVariable Integer statementId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> statementService.unconfirm(projectId, statementId), "確定を解除しました。");
	}

	// ⑦ 削除（下書きのみ）
	@DeleteMapping("/{statementId}")
	public ResponseEntity<?> delete(@PathVariable Integer projectId, @PathVariable Integer statementId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> statementService.delete(projectId, statementId), "毎次明細を削除しました。");
	}

	// ---------- 共通 ----------

	private ResponseEntity<?> run(Runnable action, String message) {
		try {
			action.run();
			return ResponseEntity.ok(Map.of("message", message));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return badRequest(e);
		}
	}

	private ResponseEntity<?> badRequest(RuntimeException e) {
		return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
	}

	private User loginAdmin(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		return (user != null && user.getRoleFlag() != null && user.getRoleFlag() == 1) ? user : null;
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("errorMessage", "権限がありません。"));
	}
}
