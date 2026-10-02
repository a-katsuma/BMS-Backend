package com.example.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Project;
import com.example.app.mapper.ProjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

	private final ProjectMapper projectMapper;

	private static final int NUM_PER_PAGE = 8;

	//	----------管理者用----------

	//	①全件取得(他ページ等で利用)
	@Override
	public List<Project> getAllProjects() {
		return projectMapper.selectAll();
	}

	// ②１件取得(編集用)
	@Override
	public Project getProjectById(Integer id) {
		return projectMapper.selectById(id);
	}

	// ③ページング付き全件取得
	@Override
	public List<Project> getAllProjects(int page) {
		int offset = (page - 1) * NUM_PER_PAGE;
		return projectMapper.selectAllWithPage(offset, NUM_PER_PAGE);
	}

	@Override
	public int getTotalPagesAll() {
		long count = projectMapper.countAll();
		return (int) Math.ceil((double) count / NUM_PER_PAGE);
	}

	// ⑤新規登録
	@Override
	public void addProject(Project project) {
		projectMapper.insert(project);
		projectMapper.touch(project.getProjectId());
	}

	// ⑥編集
	@Override
	public void editProject(Project project) {
		projectMapper.update(project);
		projectMapper.touch(project.getProjectId());
	}

	// ⑦削除（論理削除。現況確認表・ベース・明細・見積りは残す）
	@Override
	public void deleteProject(Integer id) {
		if (projectMapper.delete(id) == 0) {
			throw new IllegalArgumentException("対象の案件が見つかりません。");
		}
	}

	// 案件状態だけ更新（例外の案件）
	@Override
	public void updateStatus(Integer projectId, String status) {
		projectMapper.updateStatus(projectId, status);
	}


	// ----------発注業者用----------

	@Override
	public List<Project> getProjectsByClientIdAndCompanyId(Integer clientId, Integer companyId, int page, int limit) {
		int offset = (page - 1) * limit;
		return projectMapper.selectByClientIdAndCompanyIdWithPage(clientId, companyId, offset, limit);
	}

	@Override
	public int getTotalPagesByClientIdAndCompanyId(Integer clientId, Integer companyId, int limit) {
		long count = projectMapper.countByClientIdAndCompanyId(clientId, companyId);
		return (int) Math.ceil((double) count / limit);
	}

	// 業者管理詳細ページ関連案件取得＆ページ分割
	@Override
	public List<Project> getProjectsByCompanyId(Integer companyId, int page, int limit) {
		int offset = (page - 1) * limit;
		// 既存のMapperメソッドをそのまま使えます！
		return projectMapper.selectByCompanyIdWithPage(companyId, offset, limit);
	}

	@Override
	public int getTotalPagesByCompanyId(Integer companyId, int limit) {
		long count = projectMapper.countByCompanyId(companyId);
		// 渡された limit（この場合は3）に基づいてページ数を計算
		return (int) Math.ceil((double) count / limit);
	}

	// ----------共通？----------

	// 業者関連案件
	@Override
	public List<Project> selectByCompanyId(Integer companyId) {
		// 呼ぶのは修正した selectByCompanyId です
		return projectMapper.selectByCompanyId(companyId);
	}

	// 顧客関連案件
	@Override
	public List<Project> selectByClientId(Long clientId) {
		return projectMapper.selectByClientId(clientId);
	}

	// 顧客管理詳細ページ関連案件取得＆ページ分割
	@Override
	public List<Project> getProjectsByClientId(Integer clientId, int page, int limit) {
		int offset = (page - 1) * limit;
		// Mapperにこのメソッドを追加する必要があります（ステップ2参照）
		return projectMapper.selectByClientIdWithPage(clientId, offset, limit);
	}

	@Override
	public int getTotalPagesByClientId(Integer clientId, int limit) {
		long count = projectMapper.countByClientId(clientId);
		return (int) Math.ceil((double) count / limit);
	}
	

	// ----------削除済み（管理者用）----------

	@Override
	public List<Project> getDeletedProjects(int page) {
		int offset = (page - 1) * NUM_PER_PAGE;
		return projectMapper.selectDeletedWithPage(offset, NUM_PER_PAGE);
	}

	@Override
	public int getTotalPagesDeleted() {
		return (int) Math.ceil((double) projectMapper.countDeleted() / NUM_PER_PAGE);
	}

	@Override
	public void restoreProject(Integer id) {
		if (projectMapper.restore(id) == 0) {
			throw new IllegalArgumentException("削除済みの案件が見つかりません。");
		}
	}


}