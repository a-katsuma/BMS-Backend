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

import com.example.app.domain.Company;
import com.example.app.service.CompanyService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor // Autowiredを不要にする魔法
@RequestMapping("/companys") // URLのグループ分け
@Controller
public class CompanyController {

	// finalをつけることで、RACが自動でMapperやServiceと繋いでくれる
	private final CompanyService companyService;
	private final ProjectService projectService;
	private final QuoteService quoteService;
	private final FormatUtils fmt;

	// ① 業者一覧画面を表示
	@GetMapping("")
	public String showList(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page,
			Model model) {

		List<Company> companys;
		int totalPages;

		if (kana.isEmpty()) {
			// 全件取得
			companys = companyService.getAllCompanies(page);
			totalPages = companyService.getTotalPagesAll();
		} else {
			// カナ検索
			companys = companyService.getCompanysByKana(kana, page); // ※Serviceメソッド名は適宜合わせる
			totalPages = companyService.getTotalPagesByKana(kana);
		}

		model.addAttribute("companys", companys);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("currentPage", page);
		model.addAttribute("currentKana", kana);

		return "company/company_list";
	}

	// ② 新規登録フォーム表示
	@GetMapping("/add")
	public String showAddForm(Model model) {
		// 新規登録用オブジェクトを渡す
		model.addAttribute("companyForm", new Company());
		return "company/company_register";
	}

	// ③ 新規登録実行
	@PostMapping("/add")
	public String addCompany(@Valid @ModelAttribute("companyForm") Company company, Errors errors,
			RedirectAttributes ra) {
		// バリデーションエラーがある場合はフォーム画面へ戻る
		if (errors.hasErrors()) {
			return "company/company_register";
		}

		// 登録処理
		companyService.addCompany(company);

		// 成功メッセージをフラッシュ属性として追加
		ra.addFlashAttribute("successMessage", "業者情報を登録しました。");

		// 一覧へリダイレクト
		return "redirect:/companys";
	}

	// ④ 編集フォーム表示
	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable Integer id, Model model) {
		model.addAttribute("companyForm", companyService.getCompanyById(id));
		return "company/company_edit";
	}

	// ⑤ 更新実行
	@PostMapping("/edit/{id}")
	public String editCompany(@PathVariable Integer id, @Valid @ModelAttribute("companyForm") Company company,
			Errors errors,
			RedirectAttributes ra) {
		if (errors.hasErrors()) {
			return "company/company_edit";
		}
		company.setCompanyId(id); // URLのIDを確実にセット
		companyService.editCompany(company);
		ra.addFlashAttribute("successMessage", "業者情報を更新しました。");
		return "redirect:/companys/" + id;
	}

	// ⑥ 削除
	@GetMapping("/delete/{id}")
	public String deleteCompany(@PathVariable Integer id, RedirectAttributes ra) {
		companyService.deleteCompany(id);
		ra.addFlashAttribute("successMessage", "業者情報を削除しました。");
		return "redirect:/companys";
	}

	//⑦ 業者詳細ページ
	@GetMapping("/{id}")
	public String showCompanyDetail(
			@PathVariable Integer id,
			@RequestParam(defaultValue = "1") int page, // pageパラメータを受け取る
			Model model) {

		int limit = 3; // 業者詳細ページは3件と指定

		// 1. 業者情報を取得
		model.addAttribute("company", companyService.getCompanyById(id));

		// 2. 関連案件リストをページング付きで取得
		model.addAttribute("projects", projectService.getProjectsByCompanyId(id, page, limit));

		// 3. 総ページ数を計算して渡す
		model.addAttribute("totalPages", projectService.getTotalPagesByCompanyId(id, limit));
		model.addAttribute("currentPage", page);

		// 4. その他必要な情報
		model.addAttribute("quoteService", quoteService);

		return "company/company_detail";
	}

}
