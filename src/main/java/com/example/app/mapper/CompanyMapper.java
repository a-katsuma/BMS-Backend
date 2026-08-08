package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.Company;

@Mapper // MyBatisのMapperであることをSpringに伝えるアノテーション
public interface CompanyMapper {

	// ①全件取得(他ページ等で利用)
	List<Company> selectAll();

	// ②１件取得(編集用)
	Company selectById(int companyId);

	// ③ページング付き全件取得
	List<Company> selectAllWithPage(@Param("offset") int offset, @Param("limit") int limit);

	long countAll();

	// ④カナ範囲指定ページング付き取得
	List<Company> selectByKanaWithPage(@Param("start") String start, @Param("end") String end,
			@Param("offset") int offset, @Param("limit") int limit);

	long countByKanaRange(@Param("start") String start, @Param("end") String end);

	// ⑤新規登録
	void insert(Company company);

	// ⑥編集
	void update(Company company);

	// ⑦削除
	void delete(int companyId);
}
