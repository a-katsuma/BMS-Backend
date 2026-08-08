package com.example.app.controller.contractee;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
import com.example.app.service.ClientService;
import com.example.app.service.CompanyService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/contractee/projects")
@RequiredArgsConstructor
public class ContracteeProjectController {

	private final ProjectService projectService;
	private final ClientService clientService;
	private final CompanyService companyService;
	private final QuoteService quoteService;
	private final UserMapper userMapper; // ★疑似ログイン追加

	private final int CURRENT_USER_ID = 2;

	// 一覧表示
	@GetMapping
	public String showList(@RequestParam(defaultValue = "1") int page, Model model) {
		// 疑似ログインユーザーを取得
		User currentUser = userMapper.selectById(CURRENT_USER_ID);
		Integer companyId = currentUser.getCompanyId();

		// 8件表示に設定
		int limit = 8;

		// 修正：引数を3つ受け取る新しいメソッドを呼び出す
		model.addAttribute("projects", projectService.getProjectsByCompanyId(companyId, page, limit));

		// 修正：引数を2つ受け取る新しいメソッドを呼び出す
		model.addAttribute("totalPages", projectService.getTotalPagesByCompanyId(companyId, limit));
		model.addAttribute("currentPage", page);

		model.addAttribute("quoteService", quoteService);
		model.addAttribute("currentUser", currentUser);

		return "contractee/project_list";
	}

	// 詳細表示
	@GetMapping("/{id}")
	public String showDetail(@PathVariable Integer id, Model model) {
		User currentUser = userMapper.selectById(CURRENT_USER_ID);
		Project project = projectService.getProjectById(id);

		if (!project.getCompanyId().equals(currentUser.getCompanyId())) {
			return "redirect:/contractee/projects"; // 違う会社の案件なら一覧へ追い返す
		}

		// プロジェクト情報
		model.addAttribute("project", projectService.getProjectById(id));

		// 見積情報
		List<Quote> quotes = quoteService.getQuotesByProjectId(id);
		if (quotes != null && !quotes.isEmpty()) {
			model.addAttribute("latestQuote", quotes.get(0));
		}

		// 履歴情報
		List<Quote> historyList = quoteService.getHistoriesByProjectId(id);
		model.addAttribute("historyList", historyList);

		return "contractee/project_detail";
	}

	@PostMapping("/{projectId}/quotes/judge/{id}")
	public String judgeQuote(@PathVariable Integer projectId,
			@PathVariable Integer id,
			@RequestParam("status") String status,
			RedirectAttributes ra) {

		// 1. 判定対象のデータを取得
		Quote quote = quoteService.getQuoteById(id);

		// 2. 期限チェック
		if (quote.getDeadlineDate() != null) {
			java.time.LocalDate today = java.time.LocalDate.now();
			// 期限が今日より前の場合（＝期限切れ）は拒否
			if (quote.getDeadlineDate().isBefore(today)) {
				ra.addFlashAttribute("errorMessage", "判定期限を過ぎているため判定できません。");
				return "redirect:/contractee/projects/" + projectId;
			}
		}

		// 3. 正常な場合の更新処理
		User currentUser = userMapper.selectById(2); // 疑似ユーザー
		quote.setQuoteStatus(status);
		quote.setJudgeUser(currentUser.getName());
		quoteService.updateStatus(quote);
		quoteService.addHistory(quote);

		ra.addFlashAttribute("successMessage", "見積を「" + status + "」に更新しました。");
		return "redirect:/contractee/projects/" + projectId;
	}

}
