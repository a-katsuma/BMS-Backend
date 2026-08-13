package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController // JSONを返すコントローラーにする
@RequiredArgsConstructor
@RequestMapping("/api/home") // API用のURLに変更
@CrossOrigin(origins = "http://localhost:3000") // Reactからのアクセスを許可
public class HomeController {

	private final QuoteService quoteService;

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
}