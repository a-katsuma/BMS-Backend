package com.example.app.service;

import java.time.LocalDate;
import java.util.List;

import com.example.app.domain.ExtraWork;
import com.example.app.dto.ExtraWorkCreateRequest;

public interface ExtraWorkService {

	// ①全体の一覧（openOnly：未請求・未取り込みだけ）
	List<ExtraWork> searchExtraWorks(Integer clientId, Integer companyId, boolean openOnly);

	// ②案件の作業（1案件＝1作業。行付き。なければ null）
	ExtraWork getByProjectId(Integer projectId);

	// ③1件（行付き。見つからない・別の案件なら null）
	ExtraWork getExtraWork(Integer projectId, Integer extraWorkId);

	// ④新規受注：例外の案件と作業（下書き）を一緒に作る
	ExtraWork createWithProject(ExtraWorkCreateRequest request, String adminName);

	// ⑤保存（受注済みを保存して保留に戻したら true）
	boolean save(Integer projectId, Integer extraWorkId, ExtraWork form);

	// ⑥受注（お知らせメールを送った代表ユーザーの人数を返す）
	int order(Integer projectId, Integer extraWorkId, String orderedBy);

	// ⑦保留（下書きから）
	void hold(Integer projectId, Integer extraWorkId);

	// ⑧下書きに戻す（受注済み・未取り込み・未請求のときだけ）
	void revertToDraft(Integer projectId, Integer extraWorkId);

	// ⑨削除（例外の案件なら案件も論理削除する。案件も消したら true）
	boolean delete(Integer projectId, Integer extraWorkId);

	// ⑩請求済みにする（単独の請求書で請求したとき。案件は完了にする）
	void markBilled(Integer projectId, Integer extraWorkId, LocalDate billedDate);

	// ⑪請求済みを取り消す（案件は進行中に戻す）
	void unmarkBilled(Integer projectId, Integer extraWorkId);

	// ⑫毎次明細に取り込める作業（同じ顧客・業者で、受注済み・未請求・未取り込み。行付き）
	List<ExtraWork> getImportCandidates(Integer clientId, Integer companyId);

	// ⑬1件（案件を問わない。行なし。取り込み時の確認用）
	ExtraWork getById(Integer extraWorkId);

	// ⑭例外の案件の状態を、請求・取り込みに合わせる（どちらか済んでいれば完了、どちらもなければ進行中）
	void syncProjectStatus(Integer extraWorkId);

}
