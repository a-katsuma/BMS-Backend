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

import com.example.app.domain.ExtraWork;
import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.domain.User;
import com.example.app.service.BillingBaseService;
import com.example.app.service.ExtraWorkService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/projects")
@RequiredArgsConstructor
public class ContracteeProjectController {

	private final ProjectService projectService;
	private final QuoteService quoteService;
	private final BillingBaseService billingBaseService;
	private final ExtraWorkService extraWorkService;

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
		Quote latestQuote = quotes.isEmpty() ? null : quotes.get(0); // 先頭が最新の見積り

		// 2. quote_histories から専用の履歴リストを取得する
		List<Quote> historyList = quoteService.getHistoriesByProjectId(id, false); // 業者には削除済みの見積りの履歴を見せない


		Map<String, Object> response = new HashMap<>();
		response.put("project", project);
		response.put("latestQuote", latestQuote);
		// 過去の受注：最新以外で、発注になった見積りだけ（新しい順）。差戻し・失注の見積りは判定履歴から見る
		response.put("pastQuotes", quotes.stream().skip(1).filter(q -> "発注".equals(q.getQuoteStatus())).toList());
		response.put("historyList", historyList);
	    response.put("billingSummary", billingBaseService.getBillingSummary(id, false)); // 業者向けの項目だけ

		// 3. 例外の案件（緊急・追加作業）：受注済みの作業だけ見せる（下書き・保留は null）
		if (Project.ORDER_ROUTE_POLICY.equals(project.getOrderRoute())) {
			ExtraWork work = extraWorkService.getByProjectId(id);
			response.put("extraWork",
					(work != null && ExtraWork.STATUS_ORDERED.equals(work.getStatus())) ? work : null);
		}

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
	        return ResponseEntity.badRequest().body(Map.of("errorMessage", "ステータスが指定されていません"));
	    }

	    Quote quote = quoteService.getQuoteById(id);
	    if (quote == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("errorMessage", "見積データが見つかりません"));
	    }

	    Project project = projectService.getProjectById(quote.getProjectId());
	    if (project == null || !project.getCompanyId().equals(currentUser.getCompanyId())) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body(Map.of("errorMessage", "この見積りは判定できません"));
	    }

	    if (!quote.getProjectId().equals(projectId)) {
	        return ResponseEntity.badRequest().body(Map.of("errorMessage", "案件IDと見積りが一致しません"));
	    }

	    // 最新・未判定・期限内の見積りだけ判定できる
	    String blockReason = quoteService.checkJudgeable(projectId, id, status);
	    if (blockReason != null) {
	        return ResponseEntity.badRequest().body(Map.of("errorMessage", blockReason));
	    }


	    quote.setQuoteStatus(status);
	    quote.setJudgeUser(currentUser.getName());
	    quote.setJudgeUserId(currentUser.getUserId());
	    quoteService.updateStatus(quote);
	    quoteService.addHistory(quote);

	    return ResponseEntity.ok(Map.of("message", "見積判定を更新しました"));
	}
}
