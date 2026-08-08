package com.example.app.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.service.ClientService;
import com.example.app.service.CompanyService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor // Autowiredを不要にする魔法
@RequestMapping("/projects") // URLのグループ分け
public class ProjectController {

	//1ページ当たりの備品表示件数
	// private final int NUM_PER_PAGE = 5;

	private final ProjectService projectService;
	private final ClientService clientService;
	private final CompanyService companyService; // 追加しました
	private final QuoteService quoteService;

	// ① 一覧表示
	@GetMapping
	public String showList(@RequestParam(defaultValue = "1") int page, Model model) {
		model.addAttribute("projects", projectService.getAllProjects(page));
		model.addAttribute("totalPages", projectService.getTotalPagesAll());
		model.addAttribute("currentPage", page);
		model.addAttribute("quoteService", quoteService);
		return "project/project_list";
	}

	// ② 登録画面表示
	@GetMapping("/add")
	public String showAddForm(Model model) {
		model.addAttribute("projectForm", new Project());
		model.addAttribute("clients", clientService.getAllClients());
		model.addAttribute("companies", companyService.getAllCompanys()); // 追加しました
		return "project/project_register";
	}

	// ③ 登録処理
	@PostMapping("/add")
	public String addProject(@Valid @ModelAttribute("projectForm") Project project, Errors errors, Model model,
			RedirectAttributes ra) {
		System.out.println("バリデーションエラー数: " + errors.getErrorCount());
		if (errors.hasErrors()) {
			model.addAttribute("clients", clientService.getAllClients());
			model.addAttribute("companies", companyService.getAllCompanys()); // エラー戻り時にも必要
			return "project/project_register";
		}
		projectService.addProject(project);
		ra.addFlashAttribute("successMessage", "案件を登録しました。");
		return "redirect:/projects";
	}

	// ④ 編集フォーム
	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable Integer id, Model model) {
		model.addAttribute("projectForm", projectService.getProjectById(id));
		model.addAttribute("clients", clientService.getAllClients());
		model.addAttribute("companies", companyService.getAllCompanys()); // 追加しました
		return "project/project_edit";
	}

	//⑤ 更新処理
	@PostMapping("/edit/{id}")
	public String editProject(@PathVariable Integer id, @Valid @ModelAttribute("projectForm") Project project,
			Errors errors, Model model, RedirectAttributes ra) {
		if (errors.hasErrors()) {
			model.addAttribute("clients", clientService.getAllClients());
			model.addAttribute("companies", companyService.getAllCompanys());
			return "project/project_edit";
		}

		project.setProjectId(id); // IDを明示的にセット
		projectService.editProject(project);

		ra.addFlashAttribute("successMessage", "案件情報を更新しました。");

		// 確実に詳細画面 (/projects/1 など) へ遷移させる
		return "redirect:/projects/" + id;
	}

	// ⑥ 削除
	@GetMapping("/delete/{id}")
	public String deleteProject(@PathVariable Integer id, RedirectAttributes ra) {
		projectService.deleteProject(id);
		ra.addFlashAttribute("successMessage", "案件を削除しました。");
		return "redirect:/projects";
	}

	// ⑦ 詳細表示
	@GetMapping("/{id}")
	public String showDetail(@PathVariable Integer id, Model model) {
		model.addAttribute("project", projectService.getProjectById(id));

		// 1. 最新の見積を取得
		List<Quote> quotes = quoteService.getQuotesByProjectId(id);
		if (quotes != null && !quotes.isEmpty()) {
			model.addAttribute("latestQuote", quotes.get(0));
		}

		// 2. 過去の履歴を別テーブルから取得してセット
		List<Quote> historyList = quoteService.getHistoriesByProjectId(id);
		model.addAttribute("historyList", historyList);

		return "project/project_detail";
	}
}