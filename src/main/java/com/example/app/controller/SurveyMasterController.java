package com.example.app.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.SurveyMaster;
import com.example.app.domain.User;
import com.example.app.service.SurveyMasterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/masters")
public class SurveyMasterController {

	private final SurveyMasterService surveyMasterService;

	// ① 一覧（種別ごとにまとめて返却：{ ITEM: [...], UNIT: [...], OTHER: [...] }）
	@GetMapping
	public ResponseEntity<?> list(HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		Map<String, List<SurveyMaster>> grouped = surveyMasterService.getAll().stream()
				.collect(Collectors.groupingBy(SurveyMaster::getMasterType, LinkedHashMap::new, Collectors.toList()));
		return ResponseEntity.ok(grouped);
	}

	// ② 新規登録
	@PostMapping("/add")
	public ResponseEntity<?> add(@RequestBody SurveyMaster form, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		try {
			surveyMasterService.add(form);
			return ResponseEntity.ok(Map.of("message", "常用項目を登録しました。"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// ③ 編集
	@PostMapping("/edit/{id}")
	public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody SurveyMaster form, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		try {
			form.setMasterId(id);
			surveyMasterService.update(form);
			return ResponseEntity.ok(Map.of("message", "常用項目を更新しました。"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

	// ④ 削除
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Integer id, HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		surveyMasterService.delete(id);
		return ResponseEntity.ok(Map.of("message", "常用項目を削除しました。"));
	}

	private boolean isAdmin(HttpSession session) {
		User user = (User) session.getAttribute("LOGIN_USER");
		return user != null && user.getRoleFlag() != null && user.getRoleFlag() == 1;
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("errorMessage", "権限がありません。"));
	}
	

	// ⑤ 並び替え（direction = up / down）
	@PostMapping("/move/{id}")
	public ResponseEntity<?> move(@PathVariable Integer id, @RequestParam String direction,
			HttpSession session) {
		if (!isAdmin(session)) {
			return forbidden();
		}
		try {
			surveyMasterService.move(id, direction);
			return ResponseEntity.ok(Map.of("message", "並び順を変更しました。"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("errorMessage", e.getMessage()));
		}
	}

}
