package com.example.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.BaseDiffAlert;
import com.example.app.domain.BillingBase;
import com.example.app.domain.BillingBaseItem;
import com.example.app.domain.BillingBaseOtherItem;
import com.example.app.domain.BillingBaseVersion;

@Mapper
public interface BillingBaseMapper {

	// ①ベース（表示順・現在の版の番号と毎次明細の件数付き）
	List<BillingBase> selectByProjectId(Integer projectId);

	BillingBase selectById(Integer baseId);

	void insert(BillingBase base);

	void updateName(@Param("baseId") Integer baseId, @Param("baseName") String baseName);

	// 使用停止（stopped=true）／再開
	void updateStopped(@Param("baseId") Integer baseId, @Param("stopped") boolean stopped);

	void delete(Integer baseId);

	// ②版（新しい順・見積り情報付き）
	List<BillingBaseVersion> selectVersionsByBaseId(Integer baseId);

	void insertVersion(BillingBaseVersion version);

	void updateVersion(BillingBaseVersion version);

	void deleteVersionsByBaseId(Integer baseId);

	// ③明細
	List<BillingBaseItem> selectItemsByVersionId(Integer versionId);

	void insertItem(BillingBaseItem item);

	void deleteItemsByVersionId(Integer versionId);

	void deleteItemsByBaseId(Integer baseId);

	// ④その他項目
	List<BillingBaseOtherItem> selectOtherItemsByVersionId(Integer versionId);

	void insertOtherItem(BillingBaseOtherItem item);

	void deleteOtherItemsByVersionId(Integer versionId);

	void deleteOtherItemsByBaseId(Integer baseId);

	// ⑤差分アラートの対象（使用中のベースがある案件）
	List<BaseDiffAlert> selectAlertTargets();
}
