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

import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.service.BillingBaseService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contractee/projects/{projectId}/base")
public class ContracteeBillingBaseController {

	private final BillingBaseService billingBaseService;
	private final ProjectService projectService;

	// 閲覧のみ（全版）
	@GetMapping
	public ResponseEntity<?> show(@PathVariable Integer projectId, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Project project = projectService.getProjectById(projectId);
		if (project == null || !Objects.equals(project.getCompanyId(), currentUser.getCompanyId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("errorMessage", "権限がありません。"));
		}

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("base", billingBaseService.getBaseByProjectId(projectId));
		return ResponseEntity.ok(response);
	}
}
