package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.Client;

@Mapper
public interface ClientMapper {

	// ----------管理者用----------

	// ①全件取得
	List<Client> selectAll();

	// ②１件取得(編集用)
	Client selectById(Integer id);

	// ③ページング付き全件取得
	List<Client> selectAllWithPage(@Param("offset") int offset, @Param("num") int num);

	// データベースに登録されている顧客の総件数を取得
	//（ページングの総ページ数計算などに使用）
	long countAll();

	// ④カナ範囲指定ページング付き取得
	List<Client> selectByKanaWithPage(@Param("kanaStart") String start,
			@Param("kanaEnd") String end,
			@Param("offset") int offset,
			@Param("num") int num);

	long countByKanaRange(@Param("kanaStart") String start,
			@Param("kanaEnd") String end);

	// ④新規登録
	void insert(Client client);

	// ⑥編集
	void update(Client client);

	// ⑦削除
	void delete(Integer id);

	// 案件の件数（削除済みを含む。削除できるかの判定用）
	long countAllProjects(Integer id);

	// ----------発注業者用----------

	// 会社IDに関連する①全件取得
	List<Client> selectByCompanyId(Integer companyId);

	// 会社IDに関連する②１件取得
	Client selectByIdAndCompanyId(@Param("clientId") Integer clientId,
			@Param("companyId") Integer companyId);

	// 会社IDに関連する③ページング付き全件取得
	List<Client> selectByCompanyIdWithPage(@Param("companyId") Integer companyId,
			@Param("offset") int offset,
			@Param("num") int num);

	long countByCompanyId(@Param("companyId") Integer companyId);

	// 会社IDに関連する④カナ範囲指定ページング付き取得
	List<Client> selectByCompanyIdAndKanaWithPage(
			@Param("companyId") Integer companyId,
			@Param("kanaStart") String start,
			@Param("kanaEnd") String end,
			@Param("offset") int offset,
			@Param("num") int num);

	long countByCompanyIdAndKana(@Param("companyId") Integer companyId,
			@Param("kanaStart") String start,
			@Param("kanaEnd") String end);

}
