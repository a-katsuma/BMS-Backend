package com.example.app.controller.contractee;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
import com.example.app.service.ClientService;
import com.example.app.service.ProjectService;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/contractee/clients")
@RequiredArgsConstructor // Autowiredを不要にする魔法
public class ContracteeClientController {

	private final ClientService clientService;
	private final ProjectService projectService;
	private final FormatUtils fmt;
	private final UserMapper userMapper; // 疑似ログイン用

	//発注業者向け顧客一覧
	@GetMapping
	public String listClients(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page,
			Model model) {

		User currentUser = (User) model.getAttribute("currentUser");

		if (kana.isEmpty()) {
			model.addAttribute("clients", clientService.getClientsByCompanyId(currentUser.getCompanyId(), page));
			model.addAttribute("totalPages", clientService.getTotalPagesByCompanyId(currentUser.getCompanyId()));
		} else {
			String start = fmt.getKanaRangeStart(kana);
			String end = fmt.getKanaRangeEnd(kana);
			model.addAttribute("clients",
					clientService.getClientsByCompanyIdAndKana(currentUser.getCompanyId(), start, end, page));
			model.addAttribute("totalPages",
					clientService.getTotalPagesByCompanyIdAndKana(currentUser.getCompanyId(), start, end));
		}

		// ★重要：ここで必ずセットされているか確認
		model.addAttribute("currentPage", page);
		model.addAttribute("currentKana", kana);

		return "contractee/client_list";
	}

	//発注業者向け顧客詳細表示（ContracteeController.java 内）
	@GetMapping("/{id}")
	public String detailClient(@PathVariable Integer id,
			@RequestParam(defaultValue = "1") int page, // ページパラメータを追加
			Model model) {
		// 1. 顧客情報を取得
		model.addAttribute("client", clientService.getClientById(id));

		// 2. 関連案件をページ分割して取得 (1ページあたり3件とする場合)
		int limit = 3;
		model.addAttribute("projects", projectService.getProjectsByClientId(id, page, limit));
		model.addAttribute("totalPages", projectService.getTotalPagesByClientId(id, limit));
		model.addAttribute("currentPage", page);

		return "contractee/client_detail";
	}

}
