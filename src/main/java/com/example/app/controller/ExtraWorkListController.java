package com.example.app.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.User;
import com.example.app.dto.ExtraWorkCreateRequest;
import com.example.app.service.BusinessPolicyService;
import com.example.app.service.ClientService;
import com.example.app.service.CompanyService;
import com.example.app.service.ExtraWorkService;
import com.example.app.service.ProjectService;

import lombok.RequiredArgsConstructor;

// 緊急・追加作業：全体の一覧（サイドバー）と新規受注（例外の案件と作業を一緒に作る）
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/extra-works")
public class ExtraWorkListController {

	private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("10.00");

	private final ExtraWorkService extraWorkService;
	private final ClientService clientService;
	private final CompanyService companyService;
	private final ProjectService projectService;
	private final BusinessPolicyService businessPolicyService;

	// ① 全体の一覧（openOnly=true：済んでいないもの＝未請求・未取り込みだけ。顧客・業者で絞り込める）
	@GetMapping
	public ResponseEntity<?> list(@RequestParam(required = false) Integer clientId,
			@RequestParam(required = false) Integer companyId,
			@RequestParam(defaultValue = "true") boolean openOnly, HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return ResponseEntity.ok(Map.of("extraWorks",
				extraWorkService.searchExtraWorks(clientId, companyId, openOnly)));
	}

	// ② 新規受注の選択肢（顧客・業者と事前承認の履歴・顧客と業者の組み合わせ・税率の初期値）
	@GetMapping("/new")
	public ResponseEntity<?> newForm(HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		// 業者ごとの事前承認の履歴（画面で、実施日の時点の承認を判定する）
		List<Map<String, Object>> companies = companyService.getAllCompanys().stream()
				.map(c -> {
					Map<String, Object> m = new HashMap<>();
					m.put("companyId", c.getCompanyId());
					m.put("companyName", c.getCompanyName());
					m.put("companyKana", c.getCompanyKana());
					m.put("policies", businessPolicyService.getPolicyHistory(c.getCompanyId()));
					return m;
				})
				.toList();
		// 顧客と業者の組み合わせ（削除していない案件があるもの）。その顧客の案件がある業者を上に出すため
		List<Map<String, Integer>> pairs = projectService.getAllProjects().stream()
				.map(p -> Map.of("clientId", p.getClientId(), "companyId", p.getCompanyId()))
				.distinct()
				.toList();

		Map<String, Object> response = new HashMap<>();
		response.put("clients", clientService.getAllClients());
		response.put("companies", companies);
		response.put("pairs", pairs);
		response.put("defaultTaxRate", DEFAULT_TAX_RATE);
		return ResponseEntity.ok(response);
	}

	// ③ 新規受注（例外の案件と作業を一緒に作る）
	@PostMapping("/add")
	public ResponseEntity<?> create(@RequestBody ExtraWorkCreateRequest request, HttpSession session) {
		User user = loginAdmin(session);
		if (user == null) {
			return forbidden();
		}
		try {
			ExtraWork work = extraWorkService.createWithProject(request, user.getName());
			return ResponseEntity.ok(Map.of(
					"message", "緊急・追加作業を登録しました。",
					"projectId", work.getProjectId(),
					"extraWorkId", work.getExtraWorkId()));
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// ④ 毎次明細に取り込める作業（同じ顧客・業者で、受注済み・未請求・未取り込み。行付き）
	@GetMapping("/import-candidates")
	public ResponseEntity<?> importCandidates(@RequestParam Integer clientId, @RequestParam Integer companyId,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return ResponseEntity.ok(Map.of("extraWorks", extraWorkService.getImportCandidates(clientId, companyId)));
	}


	// ---------- 共通 ----------

	private User loginAdmin(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		return (user != null && user.getRoleFlag() != null && user.getRoleFlag() == 1) ? user : null;
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("errorMessage", "権限がありません。"));
	}
}
