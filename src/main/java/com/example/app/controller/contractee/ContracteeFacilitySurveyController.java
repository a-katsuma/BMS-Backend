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
import com.example.app.service.FacilitySurveyService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contractee/projects/{projectId}/survey")
public class ContracteeFacilitySurveyController {

	private final FacilitySurveyService facilitySurveyService;
	private final ProjectService projectService;

	// 閲覧のみ
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
		response.put("survey", facilitySurveyService.getSurveyByProjectId(projectId));
		return ResponseEntity.ok(response);
	}
}
