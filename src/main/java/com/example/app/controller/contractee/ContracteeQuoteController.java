package com.example.app.controller.contractee;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.app.service.QuoteService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/contractee/quotes") // 業者用はURLも分けると管理が楽です
@RequiredArgsConstructor
public class ContracteeQuoteController {

	private final QuoteService quoteService;

//	// 判定更新処理
//	@PostMapping("/{id}/status")
//	public String updateStatus(@PathVariable Integer id,
//			@RequestParam String status,
//			RedirectAttributes ra) {
//
//		// --- ★ここにバリデーションを追加★ ---
//		// 1. 更新前のデータを取得
//		var oldQuote = quoteService.getQuoteById(id);
//
//		if (oldQuote == null) {
//			ra.addFlashAttribute("errorMessage", "対象の見積データが見つかりません。");
//			return "redirect:/contractee/quotes";
//		}
//
//		// 2. 新しいステータスをセットした新しいオブジェクトを作成
//		var newQuote = oldQuote; // 既存データをコピー
//		newQuote.setQuoteStatus(status);
//		newQuote.setQuoteDate(java.time.LocalDate.now()); // 更新日も自動更新する場合
//
//		// 3. 引数2つの editQuote を呼び出す
//		quoteService.editQuote(oldQuote, newQuote);
//
//		ra.addFlashAttribute("successMessage", "見積を「" + status + "」に更新しました");
//		return "redirect:/contractee/quotes";
//	}

//	// 業者用の見積一覧画面
	@GetMapping
	public String list(Model model) {
		// 本来はログイン中の業者IDを使って絞り込むのが一般的です
		// model.addAttribute("quotes", quoteService.getQuotesByCompanyId(currentVendorId)); 
		return "contractee/quote_list";
	}

//	@PostMapping("/projects/{projectId}/quotes/judge/{id}")
//	public String judgeQuote(@PathVariable Integer projectId,
//			@PathVariable Integer id,
//			@RequestParam("status") String status,
//			RedirectAttributes ra) {
//
//		// 1. 指定の見積情報を取得
//		Quote quote = quoteService.getQuoteById(id);
//
//		// 2. ステータスを更新
//		quote.setQuoteStatus(status);
//		quoteService.updateStatus(quote);
//
//		// 3. ログ等に記録（必要に応じて）
//
//		ra.addFlashAttribute("successMessage", "見積判定を「" + status + "」として更新しました。");
//		return "redirect:/contractee/projects/" + projectId;
//	}

}
