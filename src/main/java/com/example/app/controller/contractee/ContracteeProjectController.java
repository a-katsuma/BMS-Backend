package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.domain.User;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/projects")
@RequiredArgsConstructor
public class ContracteeProjectController {

	private final ProjectService projectService;
	private final QuoteService quoteService;

	// 一覧表示
	@GetMapping
	public ResponseEntity<Map<String, Object>> getList(
			@RequestParam(defaultValue = "1") int page,
			HttpSession session) {

		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Integer companyId = currentUser.getCompanyId();

		int limit = 8;
		List<Project> projects = projectService.getProjectsByCompanyId(companyId, page, limit);
		int totalPages = projectService.getTotalPagesByCompanyId(companyId, limit);

		Map<String, Object> response = new HashMap<>();
		response.put("projects", projects);
		response.put("totalPages", totalPages);
		response.put("currentPage", page);

		return ResponseEntity.ok(response);
	}

	// 詳細表示
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getDetail(@PathVariable Integer id, HttpSession session) {
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Project project = projectService.getProjectById(id);
		if (project == null || !project.getCompanyId().equals(currentUser.getCompanyId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// 1. 案件に関連する最新の見積り情報を取得
		List<Quote> quotes = quoteService.getQuotesByProjectId(id);
		Quote latestQuote = null;
		if (quotes != null && !quotes.isEmpty()) {
			latestQuote = quotes.get(0); // 先頭を最新の見積りに
		}

		// 2. quote_histories から専用の履歴リストを取得する
		List<Quote> historyList = quoteService.getHistoriesByProjectId(id);

		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("latestQuote", latestQuote);
		response.put("historyList", historyList);

		return ResponseEntity.ok(response);
	} // ★ここで getDetail をしっかり閉じる！

	// 見積り判定処理
	@PostMapping("/{projectId}/quotes/judge/{id}")
	public ResponseEntity<?> judgeQuote(
			@PathVariable Integer projectId,
			@PathVariable Integer id,
			@RequestBody Map<String, String> requestBody,
			HttpSession session) {

		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String status = requestBody.get("status");
		if (status == null) {
			return ResponseEntity.badRequest().body("ステータスが指定されていません");
		}

		// 1. 指定の見積情報を取得
		Quote quote = quoteService.getQuoteById(id);
		if (quote == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("見積データが見つかりません");
		}

		// 2. ステータスや判定者を更新
		quote.setQuoteStatus(status);
		quote.setJudgeUser(currentUser.getName());
		quoteService.updateStatus(quote);

		// ★ 3. 判定履歴（quote_histories）にもデータを追加する
		quoteService.addHistory(quote);

		return ResponseEntity.ok(Map.of("message", "見積判定を更新しました"));
	}
}