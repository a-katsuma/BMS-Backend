package com.example.app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.app.domain.Quote;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/quotes") // 案件に紐づくパス
public class QuoteController {

	private final QuoteService quoteService;
	private final ProjectService projectService;

	//① 一覧表示（ここで全見積と、各見積の履歴をまとめて取得）
	// QuoteController.java の showList を以下で上書き
	@GetMapping
	public String showList(@PathVariable Integer projectId, Model model) {
		model.addAttribute("project", projectService.getProjectById(projectId));
		List<Quote> quotes = quoteService.getQuotesByProjectId(projectId);

		if (quotes != null) {
			for (Quote q : quotes) {
				if (q != null) {
					// ここで必ず初期化しておく
					q.setHistoryList(quoteService.getHistoriesByQuoteId(q.getQuoteId()));
				}
			}
		}
		model.addAttribute("quotes", quotes);
		model.addAttribute("projectId", projectId);
		return "quote/quote_list";
	}

	// ② 新規登録フォーム表示
	@GetMapping("/add")
	public String showAddForm(@PathVariable Integer projectId, Model model) {
		Quote quote = new Quote();
		quote.setProjectId(projectId); // 案件IDをセット
		model.addAttribute("quoteForm", quote);
		return "quote/quote_register";
	}

	//③ 新規登録実行
	@PostMapping("/add")
	public String addQuote(@PathVariable Integer projectId,
			@ModelAttribute("quoteForm") Quote quote,
			@RequestParam("file") MultipartFile file,
			@RequestParam("deadlineDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineDate, // 追加
			RedirectAttributes ra) {
		try {
			// 1. ファイル保存処理
			String uploadDir = "uploads/";
			java.nio.file.Files.createDirectories(java.nio.file.Paths.get(uploadDir));
			String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
			java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + fileName);
			java.nio.file.Files.write(path, file.getBytes());

			// 2. DB保存用データの設定
			quote.setProjectId(projectId);
			quote.setQuoteFilepath(uploadDir + fileName);
			quote.setQuoteDate(java.time.LocalDate.now());
			quote.setQuoteStatus("未判定");
			quote.setDeadlineDate(deadlineDate); // ★ここで期限をセット

			quoteService.addQuote(quote);
			ra.addFlashAttribute("successMessage", "見積情報を登録しました。");

		} catch (java.io.IOException e) {
			e.printStackTrace();
			ra.addFlashAttribute("errorMessage", "ファイルの保存に失敗しました。");
		}

		return "redirect:/projects/" + projectId;
	}

	//④ 編集フォーム表示
	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable Integer projectId, @PathVariable Integer id, Model model) {
		model.addAttribute("quoteForm", quoteService.getQuoteById(id));
		return "quote/quote_edit";
	}

	//⑤ 更新実行
	@PostMapping("/edit/{id}")
	public String editQuote(@PathVariable Integer projectId, @PathVariable Integer id,
			@Valid @ModelAttribute("quoteForm") Quote quote, // フォームから受け取ったデータ
			Errors errors,
			@RequestParam(value = "file", required = false) MultipartFile file,
			RedirectAttributes ra) throws IOException {

		// 1. カスタムバリデーション
		if (file == null || file.isEmpty()) {
			errors.rejectValue("quoteFilepath", "NotEmpty.quoteForm.quoteFilepath");
		}

		// 2. エラー判定
		if (errors.hasErrors()) {
			return "quote/quote_edit";
		}

		// 3. 正常系処理
		Quote oldQuote = quoteService.getQuoteById(id);

		String uploadDir = "uploads/";
		Files.createDirectories(Paths.get(uploadDir));
		String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

		// java.nio.file.Path を使用
		Path path = Paths.get(uploadDir + fileName);
		Files.write(path, file.getBytes());

		quote.setQuoteId(id);
		quote.setProjectId(projectId);
		quote.setQuoteFilepath(uploadDir + fileName);
		quote.setQuoteDate(java.time.LocalDate.now());

		// ★判定期限をフォームから取得してセット
		quote.setDeadlineDate(quote.getDeadlineDate());

		quoteService.editQuote(oldQuote, quote);

		ra.addFlashAttribute("successMessage", "見積情報を更新しました。");
		return "redirect:/projects/" + projectId;
	}

	// ⑥ 削除
	@GetMapping("/delete/{id}")
	public String deleteQuote(@PathVariable Integer projectId, @PathVariable Integer id, RedirectAttributes ra) {
		quoteService.deleteQuote(id);
		ra.addFlashAttribute("successMessage", "見積情報を削除しました。");
		return "redirect:/projects/" + projectId;
	}

}