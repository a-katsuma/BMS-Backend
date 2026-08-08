package com.example.app.service;

import java.util.List;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;

public interface QuoteService {

	// ----------管理者用----------

	// ①全件取得(見積待ち)
	List<Quote> getAllNeedQuoteQuotes();

	// ①全件取得(判定待ち)
	List<Quote> getAllPendingJudgmentQuotes();

	// ②１件取得(編集用)
	Quote getQuoteById(Integer id);

	// ③ページング付き全件取得(見積待ち)
	List<Quote> getAllNeedQuotes(int offset, int num);

	long getTotalPagesNeedQuote();

	// ③ページング付き全件取得(判定待ち)
	List<Quote> getAllPendingJudgments(int offset, int num);

	long getTotalPagesPendingJudgment();

	// ⑤新規登録
	void addQuote(Quote quote); // addClient に合わせる

	// ⑥編集
	// 履歴保存に対応した編集メソッド
	void editQuote(Quote oldQuote, Quote newQuote);

	// ⑦削除
	void deleteQuote(Integer id);

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

	
	//【　今ここ！　】
	
	
	//----------履歴・期限管理関連----------

	// ⑨見積IDに紐づく履歴一覧取得
	// 案件詳細→判定履歴
	List<Quote> getHistoriesByQuoteId(Integer id);

	// ⑨案件IDに紐づく履歴一覧取得
	// 案件詳細→最終更新日
	List<Quote> getHistoriesByProjectId(Integer projectId);

	// ⑩見積り期限のチェック
	// 一覧用
	List<Quote> getAllUrgentUnjudgedQuotes();

	// 個別用
	boolean isExpired(Project project);

	// 判定期限チェック
	boolean isExpired(Quote quote);

	// ----------？？？----------

	// ⑪指定した案件IDに紐づく最新の見積を取得

	//案件IDに基づく見積リストの取得
	//案件IDでリスト取得
	List<Quote> getQuotesByProjectId(Integer projectId); // 既存の命名規則に合わせるなら getQuotesByProjectId など

	// 更新順並び替え
	void touch(Integer projectId);

}