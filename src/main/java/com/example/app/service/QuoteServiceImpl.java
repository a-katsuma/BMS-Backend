package com.example.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.mapper.ProjectMapper;
import com.example.app.mapper.QuoteMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional // 途中で1個でも失敗したらロールバック
@RequiredArgsConstructor // finalがついたMapperを自動でドッキング
public class QuoteServiceImpl implements QuoteService {

	//finalをつけることで、Lombokが自動でコンストラクタを生成しQuoteMapperと繋いでくれる
	private final QuoteMapper quoteMapper;
	private final ProjectMapper projectMapper;

	// ----------管理者用----------

	// ①全件取得(見積待ち)
	@Override
	public List<Quote> getAllNeedQuoteQuotes() {
		return quoteMapper.selectNeedQuoteQuotes();
	}

	// ①全件取得(判定待ち)
	@Override
	public List<Quote> getAllPendingJudgmentQuotes() {
		return quoteMapper.selectPendingJudgmentQuotes();
	}

	// ②１件取得(編集用)
	@Override
	public Quote getQuoteById(Integer id) {
		return quoteMapper.selectById(id);
	}

	// ③ページング付き全件取得(見積待ち)
	@Override
	public List<Quote> getAllNeedQuotes(int offset, int num) {
		return quoteMapper.selectNeedQuoteWithPage(offset, num);
	}

	@Override
	public long getTotalPagesNeedQuote() {
		return quoteMapper.countNeedQuote();
	}

	// ③ページング付き全件取得(判定待ち)
	@Override
	public List<Quote> getAllPendingJudgments(int offset, int num) {
		return quoteMapper.selectPendingJudgmentWithPage(offset, num);
	}

	@Override
	public long getTotalPagesPendingJudgment() {
		return quoteMapper.countPendingJudgment();
	}

	// ⑤新規登録
	@Override
	public void addQuote(Quote quote) {
		quoteMapper.insert(quote);
		projectMapper.touch(quote.getProjectId());
	}

	// ⑥編集
	// 履歴保存に対応した編集メソッド
	@Override
	public void editQuote(Quote oldQuote, Quote newQuote) {
		// 編集されたら、ステータスを強制的に「未判定」にリセットする
		newQuote.setQuoteStatus("未判定");

		// フォームから来た deadlineDate を確実に newQuote に適用する
		// ※もし newQuote が @ModelAttribute でバインドされているなら自動的に入っていますが、
		// 念のため、nullチェックや値の引き継ぎが必要であればここで明示します。
		if (newQuote.getDeadlineDate() == null) {
			// もしフォームで期限が消された場合など、古い期限を維持したいなら以下のようにします
			newQuote.setDeadlineDate(oldQuote.getDeadlineDate());
		}

		// メインデータの更新
		// ここで newQuote にセットされた deadlineDate が Mapper 経由で DB に保存されます
		quoteMapper.update(newQuote);
		projectMapper.touch(newQuote.getProjectId());
	}

	// ⑦削除
	@Override
	public void deleteQuote(Integer id) {
		Quote quote = quoteMapper.selectById(id);
		quoteMapper.delete(id);
		if (quote != null) {
			projectMapper.touch(quote.getProjectId());
		}
	}

	// ----------発注業者用----------

	// ①全件取得(見積待ち)
	@Override
	public List<Quote> getAllNeedQuoteQuotesByCompanyId(Integer companyId) {
		return quoteMapper.selectNeedQuoteQuotesByCompanyId(companyId);
	}

	// ①全件取得(判定待ち)
	@Override
	public List<Quote> getAllPendingJudgmentQuotesByCompanyId(Integer companyId) {
		return quoteMapper.selectPendingJudgmentQuotesByCompanyId(companyId);
	}

	// ③ページング付き全件取得(見積待ち)

	@Override
	public List<Quote> getAllNeedQuotesByCompanyId(Integer companyId, int offset, int num) {
		return quoteMapper.selectNeedQuoteByCompanyIdWithPage(companyId, offset, num);
	}

	@Override
	public long getTotalPagesNeedQuoteByCompanyId(Integer companyId) {
		return quoteMapper.countNeedQuoteByCompanyId(companyId);
	}

	// ③ページング付き全件取得(判定待ち)

	@Override
	public List<Quote> getAllPendingJudgmentsByCompanyId(Integer companyId, int offset, int num) {
		return quoteMapper.selectPendingJudgmentByCompanyIdWithPage(companyId, offset, num);
	}

	@Override
	public long getTotalPagesPendingJudgmentByCompanyId(Integer companyId) {
		return quoteMapper.countPendingJudgmentByCompanyId(companyId);
	}

	// ⑧見積り判定用ステータス更新
	@Override
	public void updateStatus(Quote quote) {
		// Mapperを使用してUPDATEを実行する（Repositoryは不要）
		quoteMapper.updateStatus(quote);
		projectMapper.touch(quote.getProjectId());
	}

	@Override
	public void addHistory(Quote quote) {
		// 判定操作が行われた時だけ、このメソッドを呼ぶ
		quoteMapper.insertHistory(quote);
		projectMapper.touch(quote.getProjectId());
	}

	//----------履歴・期限管理関連----------

	// ⑨見積IDに紐づく履歴一覧取得
	// 案件詳細→判定履歴
	@Override
	public List<Quote> getHistoriesByQuoteId(Integer id) {
		return quoteMapper.selectHistoriesByQuoteId(id);
	}

	// ⑨案件IDに紐づく履歴一覧取得
	// 案件詳細→最終更新日
	@Override
	public List<Quote> getHistoriesByProjectId(Integer projectId) {
		return quoteMapper.selectHistoriesByProjectId(projectId);
	}

	// ⑩見積り期限のチェック

	//一覧用
	@Override
	public List<Quote> getAllUrgentUnjudgedQuotes() {
		// Mapperを使ってSQLを呼び出す
		return quoteMapper.selectUrgentUnjudgedQuotes();
	}

	// 個別用
	@Override
	public boolean isExpired(Project project) {
		if (project == null || project.getDeadlineDate() == null) {
			return false;
		}
		// "未判定" かつ 期限切れ かどうかを判定
		return "未判定".equals(project.getLatestQuoteStatus())
				&& project.getDeadlineDate().isBefore(java.time.LocalDate.now());
	}

	// 判定期限チェック
	@Override
	public boolean isExpired(Quote quote) {
		if (quote == null || quote.getDeadlineDate() == null) {
			return false;
		}
		// "未判定" かつ 期限切れ かどうかを判定
		return "未判定".equals(quote.getQuoteStatus())
				&& quote.getDeadlineDate().isBefore(java.time.LocalDate.now());
	}

	// ----------？？？----------

	// ⑪指定した案件IDに紐づく最新の見積を取得

	@Override
	public List<Quote> getQuotesByProjectId(Integer projectId) {
		return quoteMapper.selectQuotesByProjectId(projectId);
	}

	// ProjectMapper側の処理
	@Override
	public void touch(Integer projectId) {
		projectMapper.touch(projectId);
	}
}