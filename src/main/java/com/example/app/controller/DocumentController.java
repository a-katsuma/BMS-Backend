package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.app.domain.Document;
import com.example.app.service.ClientService;
import com.example.app.service.DocumentService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clients/{id}/documents") // ←ここから /contractee を削除
public class DocumentController {

	private final DocumentService documentService;
	private final ClientService clientService;

	// 資料一覧表示
	@GetMapping("")
	public String listDocuments(@PathVariable("id") Integer clientId, Model model) {
		model.addAttribute("client", clientService.getClientById(clientId));
		model.addAttribute("documents", documentService.getDocumentsByClientId(clientId));
		return "client/client_document";
	}
	
	// 登録処理
	@PostMapping("/add") // リクエストマッピングで /clients/{id}/documents となっているので /add だけでOK
	public String addDocument(
	        @PathVariable("id") Integer clientId,
	        @ModelAttribute Document document, 
	        @RequestParam("docRemarks") String docRemarks,
	        @RequestParam("file") MultipartFile file,
	        RedirectAttributes ra) {
	    try {
	        // 1. ファイル保存処理（QuoteControllerと同様）
	        String uploadDir = "uploads/docs/";
	        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(uploadDir));
	        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
	        java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + fileName);
	        java.nio.file.Files.write(path, file.getBytes());

	        // 2. Documentオブジェクトへのパス・ID設定
	        document.setDocFilePath(uploadDir + fileName);
	        document.setDocRemarks(docRemarks);
	        document.setClientId(clientId);
	        // ※プロジェクトIDも必要なら @RequestParam で受け取るか、セッション等から取得

	        // 3. サービス層へ保存依頼
	        documentService.addDocument(document);

	        ra.addFlashAttribute("successMessage", "資料を登録しました。");
	    } catch (java.io.IOException e) {
	        ra.addFlashAttribute("errorMessage", "ファイルの保存に失敗しました。");
	    }
	    return "redirect:/clients/" + clientId + "/documents";
	}
	
	// 削除処理
	@PostMapping("/delete/{docId}")
	public String deleteDocument(
	        @PathVariable("id") Integer clientId,
	        @PathVariable("docId") Integer docId,
	        RedirectAttributes ra) {
	    documentService.deleteDocument(docId);
	    ra.addFlashAttribute("successMessage", "資料を削除しました。");
	    return "redirect:/clients/" + clientId + "/documents";
	}

}