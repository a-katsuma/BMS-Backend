package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/home") // 親のURLが/home
public class HomeController {

	private final QuoteService quoteService;

	@GetMapping("")
	public String showHome(
			@RequestParam(defaultValue = "1") Integer qPage,
			@RequestParam(defaultValue = "1") Integer pPage,
			Model model) {
		model.addAttribute("quoteService", quoteService);
		// 見積待ち：5件/1P
		int qLimit = 5;
		model.addAttribute("needQuoteList", quoteService.getAllNeedQuotes((qPage - 1) * qLimit, qLimit));
		model.addAttribute("qTotalPages", (int) Math.ceil((double) quoteService.getTotalPagesNeedQuote() / qLimit));
		model.addAttribute("qPage", qPage);

		// 判定待ち：3件/1P
		int pLimit = 3;
		model.addAttribute("pendingList", quoteService.getAllPendingJudgments((pPage - 1) * pLimit, pLimit));
		model.addAttribute("pTotalPages", (int) Math.ceil((double) quoteService.getTotalPagesPendingJudgment() / pLimit));
		model.addAttribute("pPage", pPage);
		return "home/home";
	}
}