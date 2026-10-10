package com.example.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Company;
import com.example.app.domain.Project;
import com.example.app.domain.User;
import com.example.app.dto.CompanyRegisterRequest;
import com.example.app.service.CompanyService;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;
import com.example.app.service.UserService;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor // Autowiredを不要にする魔法
@RequestMapping("/api/companys") // Reactからのアクセス用プレフィックスに合わせる
@RestController
public class CompanyController {

	// 元のフィールドをそのまま保持
	private final CompanyService companyService;
	private final ProjectService projectService;
	private final QuoteService quoteService;
	private final FormatUtils fmt;
	private final UserService userService;

	// ① 業者一覧画面を表示 (JSON返却)
	@GetMapping("")
	public ResponseEntity<Map<String, Object>> showList(
			@RequestParam(required = false, defaultValue = "") String kana,
			@RequestParam(defaultValue = "1") int page) {

		List<Company> companys;
		int totalPages;

		if (kana.isEmpty()) {
			// 全件取得
			companys = companyService.getAllCompanies(page);
			totalPages = companyService.getTotalPagesAll();
		} else {
			// カナ検索
			companys = companyService.getCompanysByKana(kana, page);
			totalPages = companyService.getTotalPagesByKana(kana);
		}

		Map<String, Object> response = new HashMap<>();
		response.put("companys", companys);
		response.put("totalPages", totalPages);
		response.put("currentPage", page);
		response.put("currentKana", kana);

		return ResponseEntity.ok(response);
	}

	// ② 新規登録フォーム用データ取得
	@GetMapping("/add")
	public ResponseEntity<Company> showAddForm() {
		return ResponseEntity.ok(new Company());
	}

	// ③ 新規登録実行（ReactからのPOST /api/companys および /api/companys/add の両方に対応）
	@PostMapping(value = { "", "/add" })
	public ResponseEntity<?> addCompany(
	        @Valid @RequestBody CompanyRegisterRequest request,
	        Errors errors) {

	    if (errors.hasErrors()) {
	        return ResponseEntity.badRequest().body("入力内容に誤りがあります。");
	    }

	    	    try {
	        // 1. 登録処理を実行し、IDが割り当てられた Company オブジェクトを受け取る
	        Company company = companyService.addCompanyWithMaster(request);

	        Map<String, Object> response = new HashMap<>();
	        response.put("message", "発注元およびマスターアカウントを登録しました。");
	        // 2. フロントエンドが遷移に使う companyId をレスポンスに追加する
	        response.put("companyId", company.getCompanyId());

	        return ResponseEntity.ok(response);
	    } catch (IllegalStateException e) {
	        // 代表のメールアドレスの重複など
	        return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
	    }

	}
	// ④ 編集フォーム表示
	@GetMapping("/edit/{id}")
	public ResponseEntity<Company> showEditForm(@PathVariable Integer id) {
		Company company = companyService.getCompanyById(id);
		if (company == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(company);
	}

	// ⑤ 更新実行
	@PutMapping("/{id}")
	public ResponseEntity<?> editCompany(
			@PathVariable Integer id,
			@Valid @RequestBody Company company,
			Errors errors) {

		if (errors.hasErrors()) {
			return ResponseEntity.badRequest().body("入力内容に誤りがあります。");
		}

		company.setCompanyId(id); // URLのIDを確実にセット
		companyService.editCompany(company);

		Map<String, Object> response = new HashMap<>();
		response.put("message", "発注元情報を更新しました。");
		return ResponseEntity.ok(response);
	}

	// ⑥ 削除（DELETEメソッド、および従来のGET /delete/{id} の両方に対応）
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteCompanyApi(@PathVariable Integer id) {
		try {
			companyService.deleteCompany(id);
			return ResponseEntity.ok(Map.of("message", "発注元情報を削除しました。"));
		} catch (IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}


	//	@GetMapping("/delete/{id}")
	//	public ResponseEntity<?> deleteCompanyGet(@PathVariable Integer id) {
	//		companyService.deleteCompany(id);
	//		Map<String, Object> response = new HashMap<>();
	//		response.put("message", "業者情報を削除しました。");
	//		return ResponseEntity.ok(response);
	//	}

	// ⑦ 業者詳細ページ
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> showCompanyDetail(
	        @PathVariable Integer id,
	        @RequestParam(defaultValue = "1") int page) {

	    int limit = 3; // 業者詳細ページは3件と指定

	    // 1. 業者情報を取得
	    Company company = companyService.getCompanyById(id);
	    if (company == null) {
	        return ResponseEntity.notFound().build();
	    }

	    // 2. 関連案件リストをページング付きで取得
	    List<Project> projects = projectService.getProjectsByCompanyId(id, page, limit);

	    // 3. 総ページ数を計算
	    int totalPages = projectService.getTotalPagesByCompanyId(id, limit);

	    // 4. 業者に所属するユーザー一覧を取得（追加）
	    List<User> users = userService.getUsersByCompanyId(id);

	    Map<String, Object> response = new HashMap<>();
	    response.put("company", company);
	    response.put("projects", projects);
	    response.put("totalPages", totalPages);
	    response.put("currentPage", page);
	    response.put("users", users); // ★レスポンスに追加

	    response.put("hasQuoteService", quoteService != null);

	    return ResponseEntity.ok(response);
	}

}