package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.BillingStatement;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.service.BillingStatementService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contractee/projects/{projectId}/statements")
public class ContracteeBillingStatementController {

	private final BillingStatementService statementService;
	private final ProjectService projectService;

	// ① 一覧（確定のみ）
	@GetMapping
	public ResponseEntity<?> list(@PathVariable Integer projectId, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Project project = projectService.getProjectById(projectId);
		if (!isOwnProject(project, currentUser)) {
			return forbidden();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("statements", statementService.getStatements(projectId, true));
		return ResponseEntity.ok(response);
	}

	// ② 1件（確定のみ）
	@GetMapping("/{statementId}")
	public ResponseEntity<?> show(@PathVariable Integer projectId, @PathVariable Integer statementId,
			HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Project project = projectService.getProjectById(projectId);
		if (!isOwnProject(project, currentUser)) {
			return forbidden();
		}
		BillingStatement statement = statementService.getStatement(projectId, statementId);
		if (statement == null || !BillingStatement.STATUS_CONFIRMED.equals(statement.getStatus())) {
			return ResponseEntity.notFound().build();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("statement", statement);
		return ResponseEntity.ok(response);
	}

	private boolean isOwnProject(Project project, User user) {
		return project != null && Objects.equals(project.getCompanyId(), user.getCompanyId());
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("errorMessage", "権限がありません。"));
	}
}
