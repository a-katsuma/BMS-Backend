package com.example.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
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
	@PostMapping("/edit/{id}")
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

	// ⑥ 削除
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Integer id) {
		projectService.deleteProject(id);
		Map<String, String> response = new HashMap<>();
		response.put("message", "案件を削除しました。");
		return ResponseEntity.ok(response);
	}

	// ⑦ 詳細表示
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> showDetail(@PathVariable Integer id) {
		Project project = projectService.getProjectById(id);
		List<Quote> quotes = quoteService.getQuotesByProjectId(id);
		Quote latestQuote = (quotes != null && !quotes.isEmpty()) ? quotes.get(0) : null;
		List<Quote> historyList = quoteService.getHistoriesByProjectId(id);

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("latestQuote", latestQuote);
		response.put("historyList", historyList);

		return ResponseEntity.ok(response);
	}
}