package com.example.app.controller.contractee;

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
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/home")
@RequiredArgsConstructor
public class ContracteeHomeController {

	private final QuoteService quoteService;

	@GetMapping("")
	public ResponseEntity<?> showContracteeHome(
			@RequestParam(defaultValue = "1") Integer qPage,
			@RequestParam(defaultValue = "1") Integer pPage,
			HttpSession session) {

		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログインです");
		}
		Integer companyId = currentUser.getCompanyId();

		int pLimit = 5;
		int qLimit = 3;

		int pOffset = (pPage - 1) * pLimit;
		int qOffset = (qPage - 1) * qLimit;

		Map<String, Object> response = new HashMap<>();

		response.put("needQuoteList", quoteService.getAllNeedQuotesByCompanyId(companyId, qOffset, qLimit));
		response.put("pendingList", quoteService.getAllPendingJudgmentsByCompanyId(companyId, pOffset, pLimit));

		int pTotalPages = (int) Math
				.ceil((double) quoteService.getTotalPagesPendingJudgmentByCompanyId(companyId) / pLimit);
		int qTotalPages = (int) Math.ceil((double) quoteService.getTotalPagesNeedQuoteByCompanyId(companyId) / qLimit);

		response.put("pTotalPages", pTotalPages > 0 ? pTotalPages : 1);
		response.put("qTotalPages", qTotalPages > 0 ? qTotalPages : 1);

		response.put("pPage", pPage);
		response.put("qPage", qPage);
		response.put("currentUser", currentUser);

		return ResponseEntity.ok(response);
	}
}