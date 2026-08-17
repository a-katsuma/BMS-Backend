package com.example.app.controller.contractee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.domain.User;
import com.example.app.mapper.UserMapper;
import com.example.app.service.ProjectService;
import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contractee/projects") // Reactからの共通プレフィックス（例: /api を付与）に合わせます
@RequiredArgsConstructor
public class ContracteeProjectController {

    private final ProjectService projectService;
    private final QuoteService quoteService;
    private final UserMapper userMapper;

    private final int CURRENT_USER_ID = 2;

    // 一覧表示（JSONを返却）
    @GetMapping
    public ResponseEntity<Map<String, Object>> getList(@RequestParam(defaultValue = "1") int page) {
        User currentUser = userMapper.selectById(CURRENT_USER_ID);
        Integer companyId = currentUser.getCompanyId();

        int limit = 8;
        List<Project> projects = projectService.getProjectsByCompanyId(companyId, page, limit);
        int totalPages = projectService.getTotalPagesByCompanyId(companyId, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("projects", projects);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    // 詳細表示（JSONを返却）
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDetail(@PathVariable Integer id) {
        User currentUser = userMapper.selectById(CURRENT_USER_ID);
        Project project = projectService.getProjectById(id);

        if (project == null || !project.getCompanyId().equals(currentUser.getCompanyId())) {
            return ResponseEntity.status(403).build(); // 権限がない場合は403などを返却
        }

        List<Quote> quotes = quoteService.getQuotesByProjectId(id);
        Quote latestQuote = (quotes != null && !quotes.isEmpty()) ? quotes.get(0) : null;
        List<Quote> historyList = quoteService.getHistoriesByProjectId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("project", project);
        response.put("latestQuote", latestQuote);
        response.put("historyList", historyList);

        return ResponseEntity.ok(response);
    }

 // 見積判定処理
    @PostMapping("/{projectId}/quotes/judge/{id}")
    public ResponseEntity<Map<String, String>> judgeQuote(
            @PathVariable Integer projectId,
            @PathVariable Integer id,
            @RequestBody Map<String, String> requestBody) { // JSONで受け取る

        // Mapからstatusを取り出す
        String status = requestBody.get("status");

        Quote quote = quoteService.getQuoteById(id);

        if (quote.getDeadlineDate() != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (quote.getDeadlineDate().isBefore(today)) {
                return ResponseEntity.badRequest().body(Map.of("errorMessage", "判定期限を過ぎているため判定できません。"));
            }
        }

        User currentUser = userMapper.selectById(CURRENT_USER_ID);
        quote.setQuoteStatus(status);
        quote.setJudgeUser(currentUser.getName());
        quoteService.updateStatus(quote);
        quoteService.addHistory(quote);

        return ResponseEntity.ok(Map.of("successMessage", "見積を「" + status + "」しました。"));
    }
}