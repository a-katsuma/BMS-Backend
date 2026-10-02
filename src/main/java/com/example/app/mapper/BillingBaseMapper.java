package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.app.domain.BaseDiffAlert;
import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingBaseItem;
import com.example.app.domain.BillingBaseOtherItem;
import com.example.app.domain.BillingBaseVersion;

@Mapper
public interface BillingBaseMapper {

	// ①ベース
	BillingBase selectByProjectId(Integer projectId);

	void insert(BillingBase base);

	// ②版（新しい順・見積り情報付き）
	List<BillingBaseVersion> selectVersionsByBaseId(Integer baseId);

	void insertVersion(BillingBaseVersion version);

	void updateVersion(BillingBaseVersion version);

	// ③明細
	List<BillingBaseItem> selectItemsByVersionId(Integer versionId);

	void insertItem(BillingBaseItem item);

	void deleteItemsByVersionId(Integer versionId);

	// ④その他項目
	List<BillingBaseOtherItem> selectOtherItemsByVersionId(Integer versionId);

	void insertOtherItem(BillingBaseOtherItem item);
	

	// ⑤差分アラートの対象
	List<BaseDiffAlert> selectAlertTargets();


	void deleteOtherItemsByVersionId(Integer versionId);
}
