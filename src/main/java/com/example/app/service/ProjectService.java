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

	// ----------発注業者用----------

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
}
