package com.example.app.controller.contractee;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/contractee/home")
@RequiredArgsConstructor
public class ContracteeHomeController {

	private final QuoteService quoteService;
	private final UserMapper userMapper;

	@GetMapping("")
	public String showContracteeHome(
	        @RequestParam(defaultValue = "1") Integer qPage,
	        @RequestParam(defaultValue = "1") Integer pPage,
	        Model model) {
	    
	    User currentUser = userMapper.selectById(2); 
	    Integer companyId = currentUser.getCompanyId();
	    
	    // 設定
	    int pLimit = 5; // 判定待ち用
	    int qLimit = 3; // 見積り待ち用
	    
	    int pOffset = (pPage - 1) * pLimit;
	    int qOffset = (qPage - 1) * qLimit;

	    // リスト取得（※必要に応じてMapperに offset, num 対応のSQLを追加してください）
	    model.addAttribute("needQuoteList", quoteService.getAllNeedQuotesByCompanyId(companyId, qOffset, qLimit));
	    model.addAttribute("pendingList", quoteService.getAllPendingJudgmentsByCompanyId(companyId, pOffset, pLimit));
	    // ページ情報
	    model.addAttribute("pTotalPages", (int) Math.ceil((double) quoteService.getTotalPagesPendingJudgmentByCompanyId(companyId) / pLimit));
	    model.addAttribute("qTotalPages", (int) Math.ceil((double) quoteService.getTotalPagesNeedQuoteByCompanyId(companyId) / qLimit));
	    
	    model.addAttribute("pPage", pPage);
	    model.addAttribute("qPage", qPage);
	    model.addAttribute("currentUser", currentUser); // これがないとHTMLでエラーになります
	    model.addAttribute("quoteService", quoteService);
	    
	    return "contractee/home";
	}

}
