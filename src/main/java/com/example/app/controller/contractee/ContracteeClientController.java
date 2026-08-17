package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
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
	private final UserMapper userMapper; // ホーム画面と同様に UserMapper を注入

	// 発注業者向け顧客一覧（JSON返却）
	@GetMapping
	public Map<String, Object> listClients(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page) {

		// ホーム画面と同様にデータベースからID=2のユーザーを取得
		User currentUser = userMapper.selectById(2);
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

		return response;
	}

	// 発注業者向け顧客詳細表示（JSON返却）
	@GetMapping("/{id}")
	public Map<String, Object> detailClient(@PathVariable Integer id,
			@RequestParam(defaultValue = "1") int page) {
		
		Map<String, Object> response = new HashMap<>();
		int limit = 3;

		response.put("client", clientService.getClientById(id));
		response.put("projects", projectService.getProjectsByClientId(id, page, limit));
		response.put("totalPages", projectService.getTotalPagesByClientId(id, limit));
		response.put("currentPage", page);

		return response;
	}
}