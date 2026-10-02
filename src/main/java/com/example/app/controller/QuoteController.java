package com.example.app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.domain.Quote;
import com.example.app.domain.User;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/quotes") // Reactに合わせて /api を追加
public class QuoteController {

	private final QuoteService quoteService;
	private final ProjectService projectService;

	// ① 一覧表示（JSONで案件情報と見積りリストを返却）
	@GetMapping
	public ResponseEntity<Map<String, Object>> showList(@PathVariable Integer projectId) {
		List<Quote> quotes = quoteService.getQuotesByProjectId(projectId);

		if (quotes != null) {
			for (Quote q : quotes) {
				if (q != null) {
					q.setHistoryList(quoteService.getHistoriesByQuoteId(q.getQuoteId()));
				}
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("project", projectService.getProjectById(projectId));
		response.put("quotes", quotes);
		response.put("projectId", projectId);

		return ResponseEntity.ok(response);
	}

	// ② 編集用データ取得（Reactの axios.get に対応）
	@GetMapping("/{id}")
	public ResponseEntity<Quote> showEditForm(@PathVariable Integer projectId, @PathVariable Integer id) {
		Quote quote = quoteService.getQuoteById(id);
		return ResponseEntity.ok(quote);
	}

	// ③ 新規登録実行
	@PostMapping("/add")
	public ResponseEntity<?> addQuote(
			@PathVariable Integer projectId,
			@RequestParam("quoteStatus") String quoteStatus,
			@RequestParam(value = "deadlineDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineDate,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "registerAsOrdered", defaultValue = "false") boolean registerAsOrdered,
			HttpSession session) {

		// 運用中案件の取り込み用：管理者が「発注済み（過去分）」として登録
		User currentUser = (User) session.getAttribute("LOGIN_USER");
		if (registerAsOrdered && (currentUser == null || currentUser.getRoleFlag() != 1)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "権限がありません。"));
		}
		if (!registerAsOrdered && deadlineDate == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "判定期限を入力してください。"));
		}

		try {
			String uploadDir = "uploads/";

			Files.createDirectories(Paths.get(uploadDir));
			String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
			Path path = Paths.get(uploadDir + fileName);
			Files.write(path, file.getBytes());

			Quote quote = new Quote();
			quote.setProjectId(projectId);
			quote.setQuoteFilepath(uploadDir + fileName);
			quote.setQuoteDate(LocalDate.now());
			quote.setQuoteStatus(registerAsOrdered ? "発注" : quoteStatus);
			quote.setDeadlineDate(deadlineDate);

			quoteService.addQuote(quote);

			// 発注済み登録の場合は判定者と履歴を残す
			if (registerAsOrdered) {
				quote.setJudgeUser(currentUser.getName() + "（既存案件）");
				quote.setJudgeUserId(currentUser.getUserId());
				quoteService.updateStatus(quote);
				quoteService.addHistory(quote);
			}

			Map<String, String> response = new HashMap<>();

			response.put("message", "見積情報を登録しました。");
			return ResponseEntity.ok(response);

		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("message", "ファイルの保存に失敗しました。"));
		}
	}

	// ④ 更新実行（Reactの axios.post /edit/{id} に対応）
	@PostMapping("/edit/{id}")
	public ResponseEntity<?> editQuote(
			@PathVariable Integer projectId,
			@PathVariable Integer id,
			@RequestParam("quoteStatus") String quoteStatus,
			@RequestParam("deadlineDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineDate,
			@RequestParam(value = "file", required = false) MultipartFile file) {

		Quote oldQuote = quoteService.getQuoteById(id);
		if (oldQuote == null) {
			return ResponseEntity.notFound().build();
		}

		String filePathStr = oldQuote.getQuoteFilepath();

		// 新しいファイルがアップロードされた場合のみ差し替え
		if (file != null && !file.isEmpty()) {
			try {
				String uploadDir = "uploads/";
				Files.createDirectories(Paths.get(uploadDir));
				String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
				Path path = Paths.get(uploadDir + fileName);
				Files.write(path, file.getBytes());
				filePathStr = uploadDir + fileName;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseEntity.internalServerError().body(Map.of("message", "ファイルの保存に失敗しました。"));
			}
		}

		Quote newQuote = new Quote();
		newQuote.setQuoteId(id);
		newQuote.setProjectId(projectId);
		newQuote.setQuoteFilepath(filePathStr);
		newQuote.setQuoteDate(LocalDate.now());
		newQuote.setQuoteStatus(quoteStatus);
		newQuote.setDeadlineDate(deadlineDate);

		quoteService.editQuote(oldQuote, newQuote);

		Map<String, String> response = new HashMap<>();
		response.put("message", "見積情報を更新しました。");
		return ResponseEntity.ok(response);
	}

	// ⑤ 削除
	@DeleteMapping("/{id}") // または @PathVariable の変数名に合わせて quoteId など
	public ResponseEntity<Map<String, String>> deleteQuote(
			@PathVariable Integer projectId,
			@PathVariable Integer id) {
		quoteService.deleteQuote(id);
		Map<String, String> response = new HashMap<>();
		response.put("message", "見積情報を削除しました。");
		return ResponseEntity.ok(response);
	}
}