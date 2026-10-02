package com.example.app.service;

import java.util.List;

import com.example.app.domain.Project;

public interface ProjectService {

	// ----------管理者用----------

	// ①全件取得(他ページ等で利用)
	List<Project> getAllProjects();

	// ②１件取得(編集用)
	Project getProjectById(Integer id);

	// ③ページング付き全件取得
	List<Project> getAllProjects(int page);

	int getTotalPagesAll();

	// ⑤新規登録
	void addProject(Project project);

	// ⑥編集
	void editProject(Project project);

	// ⑦削除
	void deleteProject(Integer id);

	// 案件状態だけ更新（例外の案件：請求したら完了、取り消したら進行中）
	void updateStatus(Integer projectId, String status);


	// ----------発注業者用----------
	
	// ★追加
	List<Project> getProjectsByClientIdAndCompanyId(Integer clientId, Integer companyId, int page, int limit);
	int getTotalPagesByClientIdAndCompanyId(Integer clientId, Integer companyId, int limit);

	// ③ページング付き全件取得(業者関連案件)
	List<Project> getProjectsByCompanyId(Integer companyId, int page, int limit);

	int getTotalPagesByCompanyId(Integer companyId, int limit);

	// ----------共通？----------
	
	// 業者関連案件
	List<Project> selectByCompanyId(Integer companyId);

	// 顧客関連案件
	List<Project> selectByClientId(Long clientId);

	// ③ページング付き全件取得(顧客関連案件)
	List<Project> getProjectsByClientId(Integer clientId, int page, int limit);

	int getTotalPagesByClientId(Integer clientId, int limit);
	

	// ----------削除済み（管理者用）----------
	List<Project> getDeletedProjects(int page);

	int getTotalPagesDeleted();

	void restoreProject(Integer id);

}
