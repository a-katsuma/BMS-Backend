package com.example.app.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.ExtraWork;
import com.example.app.domain.ExtraWorkItem;

@Mapper
public interface ExtraWorkMapper {

	// ①作業（合計と取り込み先付き）
	List<ExtraWork> selectByProjectId(Integer projectId);

	ExtraWork selectById(Integer extraWorkId);

	void insert(ExtraWork work);

	void update(ExtraWork work);

	void updateStatus(ExtraWork work);

	void delete(Integer extraWorkId);

	// ②作業の行
	List<ExtraWorkItem> selectItemsByExtraWorkId(Integer extraWorkId);

	void insertItem(ExtraWorkItem item);

	void deleteItemsByExtraWorkId(Integer extraWorkId);

	// ③全体の一覧（openOnly：未請求・未取り込みだけ）
	List<ExtraWork> search(@Param("clientId") Integer clientId, @Param("companyId") Integer companyId,
			@Param("openOnly") boolean openOnly);

	// 請求済みにする／取り消す（取り消しは null）
	void updateBilledDate(@Param("extraWorkId") Integer extraWorkId, @Param("billedDate") LocalDate billedDate);
}
