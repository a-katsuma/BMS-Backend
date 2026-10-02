package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Client;
import com.example.app.domain.User;
import com.example.app.service.ClientService;
import com.example.app.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/clients")
@RequiredArgsConstructor
public class ContracteeDocumentController {

	private final DocumentService documentService;
	private final ClientService clientService;

	@GetMapping("/{id}/documents")
	public ResponseEntity<?> listDocuments(@PathVariable("id") Integer clientId, HttpSession session) {

	    User currentUser = (User) session.getAttribute("LOGIN_USER");
	    if (currentUser == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログインです");
	    }

	    Client client = clientService.getClientByIdAndCompanyId(clientId, currentUser.getCompanyId());
	    if (client == null) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この顧客情報にはアクセスできません");
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("client", client);
	    response.put("documents", documentService.getDocumentsByClientId(clientId));

	    return ResponseEntity.ok(response);
	}
}