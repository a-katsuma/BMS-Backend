package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.Quote;

@Mapper
public interface QuoteMapper {

	// ----------管理者用----------

	// ①全件取得（見積待ち）
	List<Quote> selectNeedQuoteQuotes();

	// ①全件取得（判定待ち）
	List<Quote> selectPendingJudgmentQuotes();

	// ②１件取得(編集用)
	Quote selectById(Integer id);

	// ③ページング付き全件取得（見積待ち）
	List<Quote> selectNeedQuoteWithPage(@Param("offset") int offset, @Param("num") int num);

	long countNeedQuote();

	// ③ページング付き全件取得（判定待ち）
	List<Quote> selectPendingJudgmentWithPage(@Param("offset") int offset, @Param("num") int num);

	long countPendingJudgment();

	// ⑤新規登録
	void insert(Quote quote);

	// ⑥編集
	void update(Quote quote);

	// ⑦削除
	void delete(Integer id);

	//----------発注業者用----------

	// 全件取得（見積待ち）
	List<Quote> selectNeedQuoteQuotesByCompanyId(Integer companyId);

	// 全件取得（判定待ち）
	List<Quote> selectPendingJudgmentQuotesByCompanyId(Integer companyId);

	// ③ページ分割用データ取得（見積待ち）
	List<Quote> selectNeedQuoteByCompanyIdWithPage(@Param("companyId") Integer companyId,
			@Param("offset") int offset,
			@Param("num") int num);

	long countNeedQuoteByCompanyId(@Param("companyId") Integer companyId);

	// ページ分割用データ取得（判定待ち）
	List<Quote> selectPendingJudgmentByCompanyIdWithPage(@Param("companyId") Integer companyId,
			@Param("offset") int offset,
			@Param("num") int num);

	long countPendingJudgmentByCompanyId(@Param("companyId") Integer companyId);

	// ⑧見積り判定用ステータス更新
	void updateStatus(Quote quote);

	// ⑧判定履歴(いつ誰が)の登録
	void insertHistory(Quote quote);

	//----------履歴・期限管理関連----------

	// ⑨見積IDに紐づく履歴一覧取得
	// 案件詳細→判定履歴
	List<Quote> selectHistoriesByQuoteId(Integer id);

	// ⑨案件IDに紐づく履歴一覧取得
	// 案件詳細→最終更新日
	List<Quote> selectHistoriesByProjectId(Integer projectId);

	// ⑩見積り期限のチェック
	List<Quote> selectUrgentUnjudgedQuotes();

	// ----------？？？----------

	// ⑪指定した案件IDに紐づく最新の見積を取得
	List<Quote> selectQuotesByProjectId(Integer projectId);

}
