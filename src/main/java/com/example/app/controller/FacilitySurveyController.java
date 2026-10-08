package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.FacilitySurvey;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.service.FacilitySurveyService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/survey")
public class FacilitySurveyController {

	private final FacilitySurveyService facilitySurveyService;
	private final ProjectService projectService;

	// ① 取得
	@GetMapping
	public ResponseEntity<?> show(@PathVariable Integer projectId, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("survey", facilitySurveyService.getSurveyByProjectId(projectId));
		return ResponseEntity.ok(response);
	}

	// ② 保存（新規作成・編集。1案件1件なので、作成と更新をまとめて PUT で受ける）
	@PutMapping
	public ResponseEntity<?> save(
			@PathVariable Integer projectId,
			@RequestBody FacilitySurvey form,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		try {
			facilitySurveyService.saveItems(projectId, form.getItems());
			return ResponseEntity.ok(Map.of("message", "現状確認表を保存しました。"));
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
