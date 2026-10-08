package com.example.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Project;
import com.example.app.domain.Quote;
import com.example.app.mapper.ProjectMapper;
import com.example.app.mapper.QuoteMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional // 途中で1個でも失敗したらロールバック
@RequiredArgsConstructor // finalがついたMapperを自動でドッキング
public class QuoteServiceImpl implements QuoteService {

	private static final String STATUS_UNJUDGED = "未判定";
	private static final Set<String> JUDGE_STATUSES = Set.of("発注", "失注", "差戻し");
	private static final String UPLOAD_DIR = "uploads"; // QuoteController の保存先と同じ

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

	// ⑤新規登録（初回・再見積り。前の見積りは過去の見積りとして残る）
	@Override
	public void addQuote(Quote quote) {
		quoteMapper.insert(quote);
		projectMapper.touch(quote.getProjectId());
	}

	// ⑤新しい見積りを登録できない理由：最新の見積りが未判定のときは不可（判定待ちが2件にならないように）
	@Override
	public String checkAddable(Integer projectId) {
		Quote latest = getLatestQuote(projectId);
		if (latest != null && STATUS_UNJUDGED.equals(latest.getQuoteStatus())) {
			return "最新の見積りが未判定のため、再見積りを登録できません。"
					+ "ファイルを差し替えたいときは、今の見積りを削除してから登録してください。";
		}
		return null;
	}

	// ⑥判定期限の変更（状態・ファイル・登録日は変えない）
	@Override
	public void updateDeadline(Integer projectId, Integer quoteId, LocalDate deadlineDate) {
		if (deadlineDate == null) {
			throw new IllegalArgumentException("判定期限を入力してください。");
		}
		Quote latest = getLatestQuote(projectId);
		if (latest == null || !Objects.equals(latest.getQuoteId(), quoteId)) {
			throw new IllegalStateException("判定期限を変更できるのは、最新の見積りだけです。");
		}
		if (!STATUS_UNJUDGED.equals(latest.getQuoteStatus())) {
			throw new IllegalStateException("判定期限を変更できるのは、未判定の見積りだけです。");
		}
		quoteMapper.updateDeadline(quoteId, deadlineDate);
		projectMapper.touch(projectId);
	}

	// ⑦論理削除（最新の見積りを削除すると、1つ前の見積りが最新になる）
	@Override
	public void softDelete(Integer projectId, Integer quoteId) {
		Quote quote = requireQuote(projectId, quoteId);
		if (quote.getDeletedAt() != null) {
			throw new IllegalStateException("この見積りはすでに削除されています。");
		}
		quoteMapper.softDelete(quoteId);
		projectMapper.touch(projectId);
	}

	// ⑦復元：今の最新が未判定で、復元すると最新が入れ替わるときは不可（判定待ちが2件にならないように）
	@Override
	public void restore(Integer projectId, Integer quoteId) {
		Quote quote = requireQuote(projectId, quoteId);
		if (quote.getDeletedAt() == null) {
			throw new IllegalStateException("この見積りは削除されていません。");
		}
		Quote latest = getLatestQuote(projectId);
		if (latest != null && STATUS_UNJUDGED.equals(latest.getQuoteStatus())
				&& quoteId > latest.getQuoteId()) {
			throw new IllegalStateException("今の最新の見積りが未判定のため、これより新しい見積りは復元できません。"
					+ "先に今の見積りを判定してもらうか、削除してください。");
		}
		quoteMapper.restore(quoteId);
		projectMapper.touch(projectId);
	}

	// ⑦物理削除：削除済みの見積りだけ。ベース明細の版が参照していれば不可
	//   判定履歴は外部キー（ON DELETE CASCADE）で一緒に消える
	//   PDF は、ほかの見積り・判定履歴が同じファイルを参照していなければ消す
	@Override
	public void purge(Integer projectId, Integer quoteId) {
		Quote quote = requireQuote(projectId, quoteId);
		if (quote.getDeletedAt() == null) {
			throw new IllegalStateException("完全に削除できるのは、削除済みの見積りだけです。先に削除してください。");
		}
		if (quoteMapper.countBaseVersionsByQuoteId(quoteId) > 0) {
			throw new IllegalStateException("ベース明細の元の見積りになっているため、完全には削除できません。");
		}
		boolean fileShared = quoteMapper.countFileReferences(quoteId, quote.getQuoteFilepath()) > 0;

		quoteMapper.purge(quoteId);
		projectMapper.touch(projectId);

		if (!fileShared) {
			deleteUploadedFile(quote.getQuoteFilepath());
		}
	}

	// ⑦削除済みの見積り
	@Override
	public List<Quote> getDeletedQuotes(Integer projectId) {
		return quoteMapper.selectDeletedByProjectId(projectId);
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

	// ⑧業者が判定できない理由
	@Override
	public String checkJudgeable(Integer projectId, Integer quoteId, String status) {
		if (!JUDGE_STATUSES.contains(status)) {
			return "判定の値が正しくありません。";
		}
		Quote latest = getLatestQuote(projectId);
		if (latest == null || !Objects.equals(latest.getQuoteId(), quoteId)) {
			return "判定できるのは最新の見積りだけです。画面を再読み込みしてください。";
		}
		if (!STATUS_UNJUDGED.equals(latest.getQuoteStatus())) {
			return "この見積りはすでに判定されています。画面を再読み込みしてください。";
		}
		if (isExpired(latest)) {
			return "判定期限が過ぎているため、判定できません。";
		}
		return null;
	}

	//----------履歴・期限管理関連----------

	// ⑨見積IDに紐づく履歴一覧取得
	// 案件詳細→判定履歴
	@Override
	public List<Quote> getHistoriesByQuoteId(Integer id) {
		return quoteMapper.selectHistoriesByQuoteId(id);
	}

	// ⑨案件IDに紐づく履歴一覧取得（includeDeleted=false なら削除済みの見積りの履歴は除く）
	// 案件詳細→判定履歴
	@Override
	public List<Quote> getHistoriesByProjectId(Integer projectId, boolean includeDeleted) {
		return quoteMapper.selectHistoriesByProjectId(projectId, includeDeleted);
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
		return STATUS_UNJUDGED.equals(project.getLatestQuoteStatus())
				&& project.getDeadlineDate().isBefore(LocalDate.now());
	}

	// 判定期限チェック
	@Override
	public boolean isExpired(Quote quote) {
		if (quote == null || quote.getDeadlineDate() == null) {
			return false;
		}
		// "未判定" かつ 期限切れ かどうかを判定
		return STATUS_UNJUDGED.equals(quote.getQuoteStatus())
				&& quote.getDeadlineDate().isBefore(LocalDate.now());
	}

	// ----------案件の見積り----------

	// ⑪案件の見積り（削除していないもの。新しい順＝先頭が最新の見積り）
	@Override
	public List<Quote> getQuotesByProjectId(Integer projectId) {
		return quoteMapper.selectQuotesByProjectId(projectId);
	}

	// ⑪案件の最新の見積り
	@Override
	public Quote getLatestQuote(Integer projectId) {
		List<Quote> quotes = quoteMapper.selectQuotesByProjectId(projectId);
		return quotes.isEmpty() ? null : quotes.get(0);
	}

	// ProjectMapper側の処理
	@Override
	public void touch(Integer projectId) {
		projectMapper.touch(projectId);
	}

	// ---------- 共通 ----------

	// 案件の見積り（削除済みを含む。別の案件の見積りならエラー）
	private Quote requireQuote(Integer projectId, Integer quoteId) {
		Quote quote = quoteMapper.selectById(quoteId);
		if (quote == null || !Objects.equals(quote.getProjectId(), projectId)) {
			throw new IllegalArgumentException("見積りが見つかりません。");
		}
		return quote;
	}

	// アップロードした PDF を消す（uploads フォルダの中のファイルだけ。消せなくても処理は止めない）
	private void deleteUploadedFile(String filepath) {
		if (filepath == null || filepath.isBlank()) {
			return;
		}
		Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
		Path target = Paths.get(filepath).toAbsolutePath().normalize();
		if (!target.startsWith(uploadDir)) {
			log.warn("uploads 以外のファイルは削除しません: {}", filepath);
			return;
		}
		try {
			Files.deleteIfExists(target);
		} catch (IOException e) {
			log.warn("見積りのファイルを削除できませんでした: {}", filepath, e);
		}
	}
}
