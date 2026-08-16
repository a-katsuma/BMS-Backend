package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/home") 
@RequiredArgsConstructor
public class ContracteeHomeController {

	private final QuoteService quoteService;
	private final UserMapper userMapper;

	@GetMapping("")
	public Map<String, Object> showContracteeHome(
	        @RequestParam(defaultValue = "1") Integer qPage,
	        @RequestParam(defaultValue = "1") Integer pPage) {
	    
	    User currentUser = userMapper.selectById(2); 
	    Integer companyId = currentUser.getCompanyId();
	    
	    // 発注業者用の表示仕様（上部：判定待ち5件、下部：見積り待ち3件）に合わせたリミット設定
	    int pLimit = 5; // 判定待ち用
	    int qLimit = 3; // 見積り待ち用
	    
	    int pOffset = (pPage - 1) * pLimit;
	    int qOffset = (qPage - 1) * qLimit;

	    // 返却用のデータ構造を作成
	    Map<String, Object> response = new HashMap<>();
	    
	    // リストデータの格納
	    response.put("needQuoteList", quoteService.getAllNeedQuotesByCompanyId(companyId, qOffset, qLimit));
	    response.put("pendingList", quoteService.getAllPendingJudgmentsByCompanyId(companyId, pOffset, pLimit));
	    
	    // ページ情報の格納
	    int pTotalPages = (int) Math.ceil((double) quoteService.getTotalPagesPendingJudgmentByCompanyId(companyId) / pLimit);
	    int qTotalPages = (int) Math.ceil((double) quoteService.getTotalPagesNeedQuoteByCompanyId(companyId) / qLimit);
	    
	    response.put("pTotalPages", pTotalPages > 0 ? pTotalPages : 1);
	    response.put("qTotalPages", qTotalPages > 0 ? qTotalPages : 1);
	    
	    response.put("pPage", pPage);
	    response.put("qPage", qPage);
	    response.put("currentUser", currentUser);
	    
	    return response;
	}
}