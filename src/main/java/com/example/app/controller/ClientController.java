package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Client;
import com.example.app.service.ClientService;
import com.example.app.service.ProjectService;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clients") // フロントと分かりやすく区別するため /api をつけるのが一般的です
@CrossOrigin(origins = "http://localhost:5173") // Reactからのアクセスを許可
public class ClientController {

	private final ClientService clientService;
	private final ProjectService projectService;
	private final FormatUtils fmt;

	// ① 一覧・検索取得 API
	@GetMapping
	public Map<String, Object> getClients(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page) {

		Map<String, Object> response = new HashMap<>();

		if (kana.isEmpty()) {
			response.put("clients", clientService.getAllClients(page));
			response.put("totalPages", clientService.getTotalPagesAll());
		} else {
			response.put("clients", clientService.getClientsByKana(kana, page));
			response.put("totalPages", clientService.getTotalPagesByKana(kana));
		}

		response.put("currentPage", page);
		response.put("currentKana", kana);

		return response; // Mapを返すと自動でJSONに変換されます
	}

	// ② 新規登録 API
	@PostMapping
	public ResponseEntity<?> addClient(@Valid @RequestBody Client client, Errors errors) {
		if (errors.hasErrors()) {
			return ResponseEntity.badRequest().body(errors.getAllErrors());
		}
		clientService.addClient(client);
		return ResponseEntity.ok(Map.of("message", "顧客情報を登録しました。"));
	}

	// ③ 編集データ取得 API
	@GetMapping("/edit/{id}")
	public Client getEditClient(@PathVariable Integer id) {
		return clientService.getClientById(id);
	}

	// ④ 更新実行 API
	@PostMapping("/edit/{id}")
	public ResponseEntity<?> editClient(@PathVariable Integer id, @Valid @RequestBody Client client, Errors errors) {
		if (errors.hasErrors()) {
			return ResponseEntity.badRequest().body(errors.getAllErrors());
		}
		clientService.editClient(client);
		return ResponseEntity.ok(Map.of("message", "顧客情報を更新しました。"));
	}

	// ⑤ 削除 API
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteClient(@PathVariable Integer id) {
		clientService.deleteClient(id);
		return ResponseEntity.ok(Map.of("message", "顧客情報を削除しました。"));
	}

	// ⑥ 顧客詳細ページ用データ取得 API
	@GetMapping("/{id}")
	public Map<String, Object> getClientDetail(@PathVariable Integer id, @RequestParam(defaultValue = "1") int page) {
		int limit = 3; // 詳細ページは3件固定
		Map<String, Object> response = new HashMap<>();
		response.put("client", clientService.getClientById(id));
		response.put("projects", projectService.getProjectsByClientId(id, page, limit));
		response.put("totalPages", projectService.getTotalPagesByClientId(id, limit));
		response.put("currentPage", page);
		return response;
	}
}