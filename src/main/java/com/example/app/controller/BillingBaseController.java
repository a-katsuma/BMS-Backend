package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.BillingBase;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.dto.BaseSaveRequest;
import com.example.app.service.BillingBaseService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/base")
public class BillingBaseController {

	private final BillingBaseService billingBaseService;
	private final ProjectService projectService;
	private final QuoteService quoteService;

	// ① 取得（全版・現況確認表の集計・見積り一覧）
	@GetMapping
	public ResponseEntity<?> show(@PathVariable Integer projectId, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}

		BillingBase base = billingBaseService.getBaseByProjectId(projectId);

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("base", base);
		response.put("preview", billingBaseService.buildPreview(projectId));
		response.put("quotes", quoteService.getQuotesByProjectId(projectId));
		response.put("blockReason", base == null ? billingBaseService.checkGeneratable(projectId) : null);
		return ResponseEntity.ok(response);
	}

	// ② 保存（生成・訂正・改版）
	@PostMapping("/save")
	public ResponseEntity<?> save(
			@PathVariable Integer projectId,
			@RequestBody BaseSaveRequest form,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		try {
			boolean isNew = billingBaseService.getBaseByProjectId(projectId) == null;
			billingBaseService.save(projectId, form);

			String message = isNew ? "ベースを生成しました。"
					: BaseSaveRequest.MODE_CORRECT.equals(form.getMode()) ? "現在の版を訂正しました。"
					: "新しい版として保存しました。";
			return ResponseEntity.ok(Map.of("message", message));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	private boolean isAdmin(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		return user != null && user.getRoleFlag() != null && user.getRoleFlag() == 1;
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("errorMessage", "権限がありません。"));
	}
}
