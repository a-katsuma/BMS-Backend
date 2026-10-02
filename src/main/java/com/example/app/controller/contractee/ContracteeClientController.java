package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Client;
import com.example.app.domain.User;
import com.example.app.service.ClientService;
import com.example.app.service.ProjectService;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/clients")
@RequiredArgsConstructor 
public class ContracteeClientController {

	private final ClientService clientService;
	private final ProjectService projectService;
	private final FormatUtils fmt;

	// 発注業者向け顧客一覧（JSON返却）
	@GetMapping
	public ResponseEntity<?> listClients(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page,
			HttpSession session) { // 引数に HttpSession を追加

		// セッションからログインユーザーを取得
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログインです");
		}
		Integer companyId = currentUser.getCompanyId();

		Map<String, Object> response = new HashMap<>();

		if (kana.isEmpty()) {
			response.put("clients", clientService.getClientsByCompanyId(companyId, page));
			response.put("totalPages", clientService.getTotalPagesByCompanyId(companyId));
		} else {
			String start = fmt.getKanaRangeStart(kana);
			String end = fmt.getKanaRangeEnd(kana);
			response.put("clients",
					clientService.getClientsByCompanyIdAndKana(companyId, start, end, page));
			response.put("totalPages",
					clientService.getTotalPagesByCompanyIdAndKana(companyId, start, end));
		}

		response.put("currentPage", page);
		response.put("currentKana", kana);

		return ResponseEntity.ok(response);
	}

	// 発注業者向け顧客詳細表示（JSON返却）
	@GetMapping("/{id}")
	public ResponseEntity<?> detailClient(@PathVariable Integer id,
	        @RequestParam(defaultValue = "1") int page,
	        HttpSession session) {

	    User currentUser = (User) session.getAttribute("LOGIN_USER");
	    if (currentUser == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログインです");
	    }

	    Client client = clientService.getClientByIdAndCompanyId(id, currentUser.getCompanyId());
	    if (client == null) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この顧客情報にはアクセスできません");
	    }

	    Map<String, Object> response = new HashMap<>();
	    int limit = 3;

	    response.put("client", client);
	    response.put("projects",
	            projectService.getProjectsByClientIdAndCompanyId(id, currentUser.getCompanyId(), page, limit));
	    response.put("totalPages",
	            projectService.getTotalPagesByClientIdAndCompanyId(id, currentUser.getCompanyId(), limit));
	    response.put("currentPage", page);

	    return ResponseEntity.ok(response);
	}
}