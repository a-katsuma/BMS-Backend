package com.example.app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	// ① 一覧表示（削除していない見積り。新しい順・判定履歴付き）
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

	// ② 1件（判定期限の変更画面用。別の案件・削除済みの見積りは 404）
	@GetMapping("/{id}")
	public ResponseEntity<Quote> showEditForm(@PathVariable Integer projectId, @PathVariable Integer id) {
		Quote quote = quoteService.getQuoteById(id);
		if (quote == null || !Objects.equals(quote.getProjectId(), projectId) || quote.getDeletedAt() != null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(quote);
	}

	// ③ 新規登録・再見積り（管理者のみ。最新の見積りが未判定のときは登録できない）
	@PostMapping("/add")
	public ResponseEntity<?> addQuote(
			@PathVariable Integer projectId,
			@RequestParam("quoteStatus") String quoteStatus,
			@RequestParam(value = "deadlineDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineDate,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "registerAsOrdered", defaultValue = "false") boolean registerAsOrdered,
			HttpSession session) {

		User currentUser = loginAdmin(session);
		if (currentUser == null) {
			return forbidden();
		}

		// 運用中案件の取り込み用：管理者が「発注済み（過去分）」として登録。見積りがまだない案件だけ
		boolean isRequote = quoteService.getLatestQuote(projectId) != null;
		if (registerAsOrdered && isRequote) {
			return badRequest("発注済みとして登録できるのは、見積りがまだない案件だけです。");
		}
		String blockReason = quoteService.checkAddable(projectId);
		if (blockReason != null) {
			return badRequest(blockReason);
		}
		if (!registerAsOrdered && deadlineDate == null) {
			return badRequest("判定期限を入力してください。");
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

			return ResponseEntity.ok(Map.of("message",
					isRequote ? "再見積りを登録しました。" : "見積情報を登録しました。"));

		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("errorMessage", "ファイルの保存に失敗しました。"));
		}
	}

	// ④ 判定期限の変更（管理者のみ。最新・未判定の見積りだけ。body：{ deadlineDate: "YYYY-MM-DD" }）
	//    ファイルを差し替えるときは、③の再見積りで新しい見積りとして登録する
	@PutMapping("/{id}")
	public ResponseEntity<?> updateDeadline(
			@PathVariable Integer projectId,
			@PathVariable Integer id,
			@RequestBody Quote form,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		try {
			quoteService.updateDeadline(projectId, id, form.getDeadlineDate());
			return ResponseEntity.ok(Map.of("message", "判定期限を変更しました。"));
		} catch (IllegalArgumentException | IllegalStateException e) {
			return badRequest(e.getMessage());
		}
	}

		// ⑤ 削除（論理削除。管理者のみ。判定履歴は残り、削除済みの見積りから復元できる）
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteQuote(
			@PathVariable Integer projectId,
			@PathVariable Integer id,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> quoteService.softDelete(projectId, id),
				"見積りを削除しました。削除済みの見積りから復元できます。");
	}

	// ⑥ 復元（管理者のみ）
	@PostMapping("/{id}/restore")
	public ResponseEntity<?> restoreQuote(
			@PathVariable Integer projectId,
			@PathVariable Integer id,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> quoteService.restore(projectId, id), "見積りを復元しました。");
	}

	// ⑦ 完全に削除（物理削除。管理者のみ。削除済みの見積りだけ。判定履歴・PDF も削除）
	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<?> purgeQuote(
			@PathVariable Integer projectId,
			@PathVariable Integer id,
			HttpSession session) {
		if (loginAdmin(session) == null) {
			return forbidden();
		}
		return run(() -> quoteService.purge(projectId, id), "見積りを完全に削除しました。");
	}


	// ---------- 共通 ----------


	private ResponseEntity<?> run(Runnable action, String message) {
		try {
			action.run();
			return ResponseEntity.ok(Map.of("message", message));
		} catch (IllegalArgumentException | IllegalStateException e) {
			return badRequest(e.getMessage());
		}
	}


	private User loginAdmin(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		return (user != null && user.getRoleFlag() != null && user.getRoleFlag() == 1) ? user : null;
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errorMessage", "権限がありません。"));
	}

	private ResponseEntity<Map<String, String>> badRequest(String message) {
		return ResponseEntity.badRequest().body(Map.of("errorMessage", message));
	}
}
