package com.example.app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.domain.Document;
import com.example.app.service.ClientService;
import com.example.app.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clients/{id}/documents")
public class DocumentController {

	private final DocumentService documentService;
	private final ClientService clientService;

	// ① 資料一覧・顧客情報取得 API
	@GetMapping
	public Map<String, Object> getDocuments(@PathVariable("id") Integer clientId) {
		Map<String, Object> response = new HashMap<>();
		response.put("client", clientService.getClientById(clientId));
		response.put("documents", documentService.getDocumentsByClientId(clientId));
		return response;
	}
	
	// ② 登録処理 API
	@PostMapping
	public ResponseEntity<?> addDocument(
	        @PathVariable("id") Integer clientId,
	        @ModelAttribute Document document, 
	        @RequestParam(value = "docRemarks", required = false) String docRemarks,
	        @RequestParam("file") MultipartFile file) {
	    try {
	        String uploadDir = "uploads/docs/";
	        Files.createDirectories(Paths.get(uploadDir));
	        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
	        Path path = Paths.get(uploadDir + fileName);
	        Files.write(path, file.getBytes());

	        document.setDocFilePath(uploadDir + fileName);
	        document.setDocRemarks(docRemarks);
	        document.setClientId(clientId);

	        documentService.addDocument(document);

	        return ResponseEntity.ok(Map.of("message", "資料を登録しました。"));
	    } catch (IOException e) {
	        return ResponseEntity.badRequest().body(Map.of("message", "ファイルの保存に失敗しました。"));
	    }
	}
	
	// ③ 削除処理 API
	@DeleteMapping("/{docId}")
	public ResponseEntity<?> deleteDocument(@PathVariable("docId") Integer docId) {
	    documentService.deleteDocument(docId);
	    return ResponseEntity.ok(Map.of("message", "資料を削除しました。"));
	}
}