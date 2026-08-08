package com.example.app.controller.contractee;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.app.domain.User;
import com.example.app.service.ClientService;
import com.example.app.service.DocumentService;
import com.example.app.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/contractee/clients/{id}/documents")
@RequiredArgsConstructor // Autowiredを不要にする魔法
public class ContracteeDocumentController {

	private final DocumentService documentService;
	private final ClientService clientService;
	private final UserService userService;

	// 資料一覧表示
	@GetMapping("")
	public String listDocuments(@PathVariable("id") Integer clientId, Model model) {

		User currentUser = (User) model.getAttribute("currentUser");
		if (currentUser == null) {
			return "redirect:/login"; // もしくはログインページへ飛ばす
		}

		model.addAttribute("client", clientService.getClientById(clientId));
		model.addAttribute("documents", documentService.getDocumentsByClientId(clientId));
		return "contractee/client_document";
	}

}
