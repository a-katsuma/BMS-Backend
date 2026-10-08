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
import org.springframework.web.bind.annotation.RequestParam;
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

	// ① 取得（ベース一覧・選んだベースの全版・現況確認表の集計・見積り一覧）
	//    baseId を省略したら、使用中の先頭のベース（なければ先頭のベース）
	@GetMapping
	public ResponseEntity<?> show(@PathVariable Integer projectId,
			@RequestParam(required = false) Integer baseId, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		Project project = projectService.getProjectById(projectId);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}

		List<BillingBase> bases = billingBaseService.getBases(projectId, false);
		BillingBase base = billingBaseService.getBase(projectId, (baseId != null) ? baseId : defaultBaseId(bases));
		if (baseId != null && base == null) {
			return ResponseEntity.notFound().build();
		}

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("bases", bases);
		response.put("base", base);
		response.put("preview", billingBaseService.buildPreview(projectId));
		response.put("otherBaseItems",
				billingBaseService.getOtherBaseItems(projectId, base == null ? null : base.getBaseId()));
		response.put("quotes", quoteService.getQuotesByProjectId(projectId));
		response.put("blockReason", billingBaseService.checkGeneratable(projectId)); // 新しいベースを作れない理由
		return ResponseEntity.ok(response);
	}

		// ②-1 新しいベースの作成（第1版。body：baseName, quoteId, taxRate, items, otherItems）
	@PostMapping
	public ResponseEntity<?> create(
			@PathVariable Integer projectId,
			@RequestBody BaseSaveRequest form,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		form.setBaseId(null);
		return save(projectId, form, "ベースを作成しました。");
	}

	// ②-2 訂正（今の版を上書き。body：quoteId, taxRate, items, otherItems）
	@PutMapping("/{baseId}")
	public ResponseEntity<?> correct(
			@PathVariable Integer projectId,
			@PathVariable Integer baseId,
			@RequestBody BaseSaveRequest form,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		form.setBaseId(baseId);
		form.setMode(BaseSaveRequest.MODE_CORRECT);
		return save(projectId, form, "現在の版を訂正しました。");
	}

	// ②-3 改版（新しい版を作る。body は訂正と同じ）
	@PostMapping("/{baseId}/versions")
	public ResponseEntity<?> revise(
			@PathVariable Integer projectId,
			@PathVariable Integer baseId,
			@RequestBody BaseSaveRequest form,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		form.setBaseId(baseId);
		form.setMode(BaseSaveRequest.MODE_REVISE);
		return save(projectId, form, "新しい版として保存しました。");
	}

	// ③ ベース名の変更（body：{ baseName }）
	@PutMapping("/{baseId}/name")
	public ResponseEntity<?> rename(@PathVariable Integer projectId, @PathVariable Integer baseId,
			@RequestBody Map<String, String> body, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		return run(() -> billingBaseService.rename(projectId, baseId, body.get("baseName")),
				"ベース名を変更しました。");
	}

	// 保存（②-1〜②-3）の共通処理。応答に保存したベースのID
	private ResponseEntity<?> save(Integer projectId, BaseSaveRequest form, String message) {
		try {
			Integer savedBaseId = billingBaseService.save(projectId, form);
			return ResponseEntity.ok(Map.of("message", message, "baseId", savedBaseId));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return badRequest(e);
		}
	}

	// ④ 使用停止
	@PostMapping("/{baseId}/stop")
	public ResponseEntity<?> stop(@PathVariable Integer projectId, @PathVariable Integer baseId,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		return run(() -> billingBaseService.setStopped(projectId, baseId, true), "使用停止にしました。");
	}

	// ⑤ 使用再開
	@PostMapping("/{baseId}/resume")
	public ResponseEntity<?> resume(@PathVariable Integer projectId, @PathVariable Integer baseId,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		return run(() -> billingBaseService.setStopped(projectId, baseId, false), "使用を再開しました。");
	}

	// ⑥ 削除（使用停止中で、毎次明細がないベースだけ）
	@DeleteMapping("/{baseId}")
	public ResponseEntity<?> delete(@PathVariable Integer projectId, @PathVariable Integer baseId,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		return run(() -> billingBaseService.delete(projectId, baseId), "ベースを削除しました。");
	}

	// ---------- 共通 ----------

	// 使用中の先頭のベース、なければ先頭のベース
	private Integer defaultBaseId(List<BillingBase> bases) {
		return bases.stream().filter(b -> !b.isStopped()).findFirst()
				.or(() -> bases.stream().findFirst())
				.map(BillingBase::getBaseId)
				.orElse(null);
	}

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

	private boolean isAdmin(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		return user != null && user.getRoleFlag() != null && user.getRoleFlag() == 1;
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("errorMessage", "権限がありません。"));
	}
}
