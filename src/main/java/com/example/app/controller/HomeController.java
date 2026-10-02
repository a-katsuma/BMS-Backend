package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.User;
import com.example.app.service.BillingBaseService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController // JSONを返すコントローラーにする
@RequiredArgsConstructor
@RequestMapping("/api/home") // API用のURLに変更
public class HomeController {

	private final QuoteService quoteService;
	private final BillingBaseService billingBaseService;

	@GetMapping("")
	public ResponseEntity<Map<String, Object>> showHome(
			@RequestParam(defaultValue = "1") Integer qPage,
			@RequestParam(defaultValue = "1") Integer pPage) {
		
		int qLimit = 5;
		int pLimit = 3;

		Map<String, Object> response = new HashMap<>();
		
		// 見積待ちデータと総ページ数
		response.put("needQuoteList", quoteService.getAllNeedQuotes((qPage - 1) * qLimit, qLimit));
		response.put("qTotalPages", (int) Math.ceil((double) quoteService.getTotalPagesNeedQuote() / qLimit));
		response.put("qPage", qPage);

		// 判定待ちデータと総ページ数
		response.put("pendingList", quoteService.getAllPendingJudgments((pPage - 1) * pLimit, pLimit));
		response.put("pTotalPages", (int) Math.ceil((double) quoteService.getTotalPagesPendingJudgment() / pLimit));
		response.put("pPage", pPage);

		return ResponseEntity.ok(response);
	}

	// 現況確認表とベース明細に差がある案件（管理者のみ）
	@GetMapping("/base-alerts")
	public ResponseEntity<?> baseAlerts(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		if (user == null || user.getRoleFlag() == null || user.getRoleFlag() != 1) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("errorMessage", "権限がありません。"));
		}
		return ResponseEntity.ok(billingBaseService.getSurveyDiffAlerts());
	}

}