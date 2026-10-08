package com.example.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.domain.User;
import com.example.app.service.BillingBaseService;
import com.example.app.service.ClientService;
import com.example.app.service.CompanyService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects") // API用のプレフィックスに変更
public class ProjectController {

	private final ProjectService projectService;
	private final ClientService clientService;
	private final CompanyService companyService;
	private final QuoteService quoteService;
	private final BillingBaseService billingBaseService;

	// ① 一覧表示 (ページング情報とプロジェクトリストをMapでまとめてJSON返却)
	@GetMapping
	public ResponseEntity<Map<String, Object>> showList(@RequestParam(defaultValue = "1") int page) {
		List<Project> projects = projectService.getAllProjects(page);
		int totalPages = projectService.getTotalPagesAll();

		Map<String, Object> response = new HashMap<>();
		response.put("projects", projects);
		response.put("totalPages", totalPages);
		response.put("currentPage", page);

		return ResponseEntity.ok(response);
	}

	// ② 登録画面用データ取得（セレクトボックス用の顧客・業者一覧を返す）
	@GetMapping("/form-data")
	public ResponseEntity<Map<String, Object>> getFormData() {
		Map<String, Object> response = new HashMap<>();
		response.put("clients", clientService.getAllClients());
		response.put("companies", companyService.getAllCompanys());
		return ResponseEntity.ok(response);
	}

	// ③ 登録処理の例
		@PostMapping("/add")
		public ResponseEntity<?> addProject(@Valid @RequestBody Project project, Errors errors) {
			if (errors.hasErrors()) {
				return ResponseEntity.badRequest().body(errors.getAllErrors());
			}

			// 登録を実行
			projectService.addProject(project);

			// もしここで project.getProjectId() が null になってしまう場合は、
			// Service側でインサート後にIDを採番・取得する仕組みが必要です。

			Map<String, Object> response = new HashMap<>();
			response.put("message", "案件を登録しました。");
			response.put("projectId", project.getProjectId());

			return ResponseEntity.ok(response);
		}

	// ④ 編集用データ取得
	@GetMapping("/edit/{id}")
	public ResponseEntity<Map<String, Object>> showEditForm(@PathVariable Integer id) {
		Map<String, Object> response = new HashMap<>();
		response.put("project", projectService.getProjectById(id));
		response.put("clients", clientService.getAllClients());
		response.put("companies", companyService.getAllCompanys());
		return ResponseEntity.ok(response);
	}

	// ⑤ 更新処理
	@PutMapping("/{id}")
	public ResponseEntity<?> editProject(@PathVariable Integer id, @Valid @RequestBody Project project, Errors errors) {
		if (errors.hasErrors()) {
			return ResponseEntity.badRequest().body(errors.getAllErrors());
		}

		project.setProjectId(id);
		projectService.editProject(project);

		Map<String, String> response = new HashMap<>();
		response.put("message", "案件情報を更新しました。");
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Integer id) {
		try {
			projectService.deleteProject(id);
			return ResponseEntity.ok(Map.of("message", "案件を削除しました。"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// ⑥-2 削除済みの案件一覧（管理者のみ）
	@GetMapping("/deleted")
	public ResponseEntity<?> showDeletedList(@RequestParam(defaultValue = "1") int page, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("projects", projectService.getDeletedProjects(page));
		response.put("totalPages", projectService.getTotalPagesDeleted());
		response.put("currentPage", page);
		return ResponseEntity.ok(response);
	}

	// ⑥-3 復元（管理者のみ）
	@PostMapping("/{id}/restore")
	public ResponseEntity<?> restoreProject(@PathVariable Integer id, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		try {
			projectService.restoreProject(id);
			return ResponseEntity.ok(Map.of("message", "案件を復元しました。"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}


	// ⑦ 詳細表示
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> showDetail(@PathVariable Integer id) {
		Project project = projectService.getProjectById(id);
		List<Quote> quotes = quoteService.getQuotesByProjectId(id);
		Quote latestQuote = quotes.isEmpty() ? null : quotes.get(0);
		List<Quote> historyList = quoteService.getHistoriesByProjectId(id, true); // 削除済みの見積りの履歴も（印付き）

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("latestQuote", latestQuote);
		// 過去の受注：最新以外で、発注になった見積りだけ（新しい順）。差戻し・失注の見積りは判定履歴から見る
		response.put("pastQuotes", quotes.stream().skip(1).filter(q -> "発注".equals(q.getQuoteStatus())).toList());
		response.put("deletedQuotes", quoteService.getDeletedQuotes(id));
		response.put("historyList", historyList);
		response.put("billingSummary", billingBaseService.getBillingSummary(id, true));

		return ResponseEntity.ok(response);
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