package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.service.BusinessPolicyService;
import com.example.app.service.ExtraWorkService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

// 例外の案件（緊急・追加作業）の作業の操作。一覧と新規受注は ExtraWorkListController
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/extra-works")
public class ExtraWorkController {

	private final ExtraWorkService extraWorkService;
	private final ProjectService projectService;
	private final BusinessPolicyService businessPolicyService;

	// ① 案件の作業（1案件＝1作業。案件詳細用。なければ extraWork は null）
	@GetMapping
	public ResponseEntity<?> byProject(@PathVariable Integer projectId, HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("extraWork", extraWorkService.getByProjectId(projectId));
		return ResponseEntity.ok(response);
	}

	// ② 1件（編集・プレビュー）
	@GetMapping("/{extraWorkId}")
	public ResponseEntity<?> show(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		ExtraWork work = extraWorkService.getExtraWork(projectId, extraWorkId);
		if (project == null || work == null) {
			return ResponseEntity.notFound().build();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("extraWork", work);
		response.put("policies", businessPolicyService.getPolicyHistory(project.getCompanyId())); // 実施日時点の事前承認の判定用
		return ResponseEntity.ok(response);
	}

	// ③ 保存
	@PostMapping("/{extraWorkId}/save")
	public ResponseEntity<?> save(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			@RequestBody ExtraWork form, HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		try {
			boolean backToPending = extraWorkService.save(projectId, extraWorkId, form);
			return ResponseEntity.ok(Map.of("message", backToPending
					? "保存しました。受注済みだったため保留に戻しました。もう一度受注してください。"
					: "保存しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return badRequest(e);
		}
	}

	// ④ 受注
	@PostMapping("/{extraWorkId}/order")
	public ResponseEntity<?> order(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			HttpSession session) {
		User user = loginAdmin(session);
		if (user == null) {
			return forbidden();
		}
		try {
			int sent = extraWorkService.order(projectId, extraWorkId, user.getName());
			return ResponseEntity.ok(Map.of("message", sent > 0
					? "受注しました。業者の代表ユーザーにお知らせメールを送りました。"
					: "受注しました。（お知らせメールの送り先となる代表ユーザーがいません）"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return badRequest(e);
		}
	}

	// ⑤ 保留
	@PostMapping("/{extraWorkId}/hold")
	public ResponseEntity<?> hold(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> extraWorkService.hold(projectId, extraWorkId), "保留にしました。");
	}

	// ⑥ 下書きに戻す
	@PostMapping("/{extraWorkId}/revert")
	public ResponseEntity<?> revert(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> extraWorkService.revertToDraft(projectId, extraWorkId), "下書きに戻しました。");
	}

	// ⑦ 請求済みにする（body：{ billedDate: "YYYY-MM-DD" }）
	@PostMapping("/{extraWorkId}/billed")
	public ResponseEntity<?> billed(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			@RequestBody ExtraWork form, HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> extraWorkService.markBilled(projectId, extraWorkId, form.getBilledDate()),
				"請求済みにしました。案件を完了にしました。");
	}

	// ⑧ 請求済みを取り消す
	@PostMapping("/{extraWorkId}/unbilled")
	public ResponseEntity<?> unbilled(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> extraWorkService.unmarkBilled(projectId, extraWorkId),
				"請求済みを取り消しました。案件を進行中に戻しました。");
	}

	// ⑨ 削除（例外の案件なら案件も削除）
	@DeleteMapping("/{extraWorkId}")
	public ResponseEntity<?> delete(@PathVariable Integer projectId, @PathVariable Integer extraWorkId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		try {
			boolean projectDeleted = extraWorkService.delete(projectId, extraWorkId);
			return ResponseEntity.ok(Map.of("message", projectDeleted
					? "緊急・追加作業と案件を削除しました。（案件は削除済み一覧から復元できます）"
					: "緊急・追加作業を削除しました。"));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return badRequest(e);
		}
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
