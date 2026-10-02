package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.BillingStatement;
import com.example.app.domain.BillingStatementItem;
import com.example.app.domain.BillingStatementOtherItem;

@Mapper
public interface BillingStatementMapper {

	// ①毎次明細（一覧は新しい年月順・小計付き）
	List<BillingStatement> selectByProjectId(@Param("projectId") Integer projectId,
			@Param("confirmedOnly") boolean confirmedOnly);

	BillingStatement selectById(Integer statementId);

	void insert(BillingStatement statement);

	void updateStatus(BillingStatement statement);

	void touch(Integer statementId);

	void delete(Integer statementId);

	// ②明細行
	List<BillingStatementItem> selectItemsByStatementId(Integer statementId);

	void insertItem(BillingStatementItem item);

	void updateItemAdjustment(BillingStatementItem item);

	void deleteItemsByStatementId(Integer statementId);
	

	void deleteTemporaryItems(Integer statementId);


	// ③その他項目
	List<BillingStatementOtherItem> selectOtherItemsByStatementId(Integer statementId);

	void insertOtherItem(BillingStatementOtherItem item);

	void updateOtherItem(BillingStatementOtherItem item);

	void deleteOtherItemsByStatementId(Integer statementId);
}
