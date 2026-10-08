package com.example.app.service;

import java.time.LocalDate;
import java.util.List;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;

public interface QuoteService {

	// ----------管理者用----------

	// ①全件取得(見積待ち)
	List<Quote> getAllNeedQuoteQuotes();

	// ①全件取得(判定待ち)
	List<Quote> getAllPendingJudgmentQuotes();

	// ②１件取得(編集用。削除済みも返す)
	Quote getQuoteById(Integer id);

	// ③ページング付き全件取得(見積待ち)
	List<Quote> getAllNeedQuotes(int offset, int num);

	long getTotalPagesNeedQuote();

	// ③ページング付き全件取得(判定待ち)
	List<Quote> getAllPendingJudgments(int offset, int num);

	long getTotalPagesPendingJudgment();

	// ⑤新規登録（初回・再見積り）
	void addQuote(Quote quote); // addClient に合わせる

	// ⑤新しい見積りを登録できない理由（登録できるなら null）
	String checkAddable(Integer projectId);

	// ⑥判定期限の変更（最新・未判定の見積りだけ）
	void updateDeadline(Integer projectId, Integer quoteId, LocalDate deadlineDate);


	// ⑦論理削除（判定履歴は残す）
	void softDelete(Integer projectId, Integer quoteId);

	// ⑦復元
	void restore(Integer projectId, Integer quoteId);

	// ⑦物理削除（削除済みの見積りだけ。判定履歴・PDF も削除）
	void purge(Integer projectId, Integer quoteId);

	// ⑦削除済みの見積り（管理者用）
	List<Quote> getDeletedQuotes(Integer projectId);

	// ----------発注業者用----------

	// ①全件取得(見積待ち)
	List<Quote> getAllNeedQuoteQuotesByCompanyId(Integer companyId);

	// ①全件取得(判定待ち)
	List<Quote> getAllPendingJudgmentQuotesByCompanyId(Integer companyId);

	// ③ページング付き全件取得(見積待ち)

	List<Quote> getAllNeedQuotesByCompanyId(Integer companyId, int offset, int num);

	long getTotalPagesNeedQuoteByCompanyId(Integer companyId);

	// ③ページング付き全件取得(判定待ち)
	List<Quote> getAllPendingJudgmentsByCompanyId(Integer companyId, int offset, int num);

	long getTotalPagesPendingJudgmentByCompanyId(Integer companyId);

	// ⑧見積り判定用ステータス更新
	void updateStatus(Quote quote);

	void addHistory(Quote quote);

	// ⑧業者が判定できない理由（判定できるなら null）：最新・未判定・期限内の見積りだけ判定できる
	String checkJudgeable(Integer projectId, Integer quoteId, String status);

	//----------履歴・期限管理関連----------

	// ⑨見積IDに紐づく履歴一覧取得
	// 案件詳細→判定履歴
	List<Quote> getHistoriesByQuoteId(Integer id);

	// ⑨案件IDに紐づく履歴一覧取得
	// 案件詳細→最終更新日
	List<Quote> getHistoriesByProjectId(Integer projectId, boolean includeDeleted);

	// ⑩見積り期限のチェック
	// 一覧用
	List<Quote> getAllUrgentUnjudgedQuotes();

	// 個別用
	boolean isExpired(Project project);

	// 判定期限チェック
	boolean isExpired(Quote quote);

	// ----------案件の見積り----------

	// ⑪案件の見積り（削除していないもの。新しい順＝先頭が最新の見積り）
	List<Quote> getQuotesByProjectId(Integer projectId);

	// ⑪案件の最新の見積り（なければ null）
	Quote getLatestQuote(Integer projectId);

	// 更新順並び替え
	void touch(Integer projectId);

}
