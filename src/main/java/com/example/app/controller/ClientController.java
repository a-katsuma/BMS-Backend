package com.example.app.controller;

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

import com.example.app.domain.Client;
import com.example.app.service.ClientService;
import com.example.app.service.ProjectService;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor // Autowiredを不要にする魔法
@RequestMapping("/clients") // URLのグループ分け
public class ClientController {

	//1ページ当たりの備品表示件数
	// private final int NUM_PER_PAGE = 5;

	//finalをつけることで、Lombokが自動でServiceと繋いでくれる
	private final ClientService clientService;
	private final ProjectService projectService;
	private final FormatUtils fmt;

	// ① 一覧表示
	//	@GetMapping
	//	public String showList(Model model) {
	//		model.addAttribute("clients", clientService.getAllClients());
	//		return "client/client_list";
	//	}

	@GetMapping
	public String showList(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page,
			Model model) {

		// 1. カナが空なら全件取得
		if (kana.isEmpty()) {
			model.addAttribute("clients", clientService.getAllClients(page));
			model.addAttribute("totalPages", clientService.getTotalPagesAll());
		} else {
			// 2. カナがあるなら検索用メソッドを呼び出す
			model.addAttribute("clients", clientService.getClientsByKana(kana, page));
			model.addAttribute("totalPages", clientService.getTotalPagesByKana(kana));
		}

		model.addAttribute("currentPage", page);
		model.addAttribute("currentKana", kana);

		return "client/client_list";
	}

	// ② 新規登録フォーム表示
	@GetMapping("/add")
	public String showAddForm(Model model) {
		model.addAttribute("clientForm", new Client());
		return "client/client_register";
	}

	// ③ 新規登録実行
	@PostMapping("/add")
	public String addClient(@Valid @ModelAttribute("clientForm") Client client, Errors errors, RedirectAttributes ra) {
		if (errors.hasErrors()) {
			return "client/client_register";
		}
		clientService.addClient(client);
		ra.addFlashAttribute("successMessage", "顧客情報を登録しました。");
		return "redirect:/clients";
	}

	// ④ 編集フォーム表示
	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable Integer id, Model model) {
		model.addAttribute("clientForm", clientService.getClientById(id));
		return "client/client_edit";
	}

	// ⑤ 更新実行
	@PostMapping("/edit/{id}")
	public String editClient(@PathVariable Integer id, @Valid @ModelAttribute("clientForm") Client client, Errors errors,
			RedirectAttributes ra) {
		if (errors.hasErrors()) {
			return "client/client_edit";
		}
		clientService.editClient(client);
		ra.addFlashAttribute("successMessage", "顧客情報を更新しました。");
		return "redirect:/clients/" + id;
	}

	// ⑥ 削除
	@GetMapping("/delete/{id}")
	public String deleteClient(@PathVariable Integer id, RedirectAttributes ra) {
		clientService.deleteClient(id);
		ra.addFlashAttribute("successMessage", "顧客情報を削除しました。");
		return "redirect:/clients";
	}

	// ⑦ 顧客詳細ページ
	@GetMapping("/{id}")
	public String showClientDetail(@PathVariable Integer id, @RequestParam(defaultValue = "1") int page, Model model) {
		int limit = 3; // 詳細ページは3件固定
		model.addAttribute("client", clientService.getClientById(id));
		model.addAttribute("projects", projectService.getProjectsByClientId(id, page, limit));
		model.addAttribute("totalPages", projectService.getTotalPagesByClientId(id, limit));
		model.addAttribute("currentPage", page);
		return "client/client_detail";
	}

}