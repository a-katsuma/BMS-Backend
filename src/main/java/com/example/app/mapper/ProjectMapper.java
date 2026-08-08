package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.Project;

@Mapper
public interface ProjectMapper {

	// ----------管理者用----------

	// ①全件取得(他ページ等で利用)
	List<Project> selectAll();

	// ②１件取得(編集用)
	Project selectById(Integer id);

	// ③ページング付き全件取得
	List<Project> selectAllWithPage(@Param("offset") int offset, @Param("num") int num);

	long countAll();

	// ⑤新規登録
	void insert(Project project);

	// ⑥編集
	void update(Project project);

	// ⑦削除
	void delete(Integer id);

	// ----------発注業者用----------

	// ※業者管理詳細ページ 関連案件取得＆ページ分割でも使用？
	// ③ページング付き全件取得
	List<Project> selectByCompanyIdWithPage(@Param("companyId") Integer companyId, @Param("offset") int offset,
			@Param("num") int num);

	long countByCompanyId(@Param("companyId") Integer companyId);

	// ----------共通？----------

	// 見積り更新時案件の最終更新日も更新
	void touch(Integer projectId);

	// ①全件取得 業者関連案件の取得
	// [管理者用]→業者詳細ページ→関連案件
	// [発注業者用]→案件一覧ページ
	List<Project> selectByCompanyId(Integer companyId);

	// ①全件取得 顧客関連案件の取得
	List<Project> selectByClientId(Long clientId);

	// ③ページング付き全件取得
	// 顧客管理詳細ページ 関連案件取得＆ページ分割
	List<Project> selectByClientIdWithPage(@Param("clientId") Integer clientId, @Param("offset") int offset,
			@Param("num") int num);
	long countByClientId(@Param("clientId") Integer clientId);

}
