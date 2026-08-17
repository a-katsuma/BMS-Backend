package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
import com.example.app.service.ClientService;
import com.example.app.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/clients")
@RequiredArgsConstructor 
public class ContracteeDocumentController {

	private final DocumentService documentService;
	private final ClientService clientService;
	private final UserMapper userMapper; // 他のコントローラーと同様に UserMapper を注入

	// 発注業者向け資料一覧表示（JSON返却）
	@GetMapping("/{id}/documents")
	public Map<String, Object> listDocuments(@PathVariable("id") Integer clientId) {

		// ログインユーザーの確認（必要に応じて使用）
		User currentUser = userMapper.selectById(2);

		Map<String, Object> response = new HashMap<>();
		response.put("client", clientService.getClientById(clientId));
		response.put("documents", documentService.getDocumentsByClientId(clientId));

		return response;
	}
}