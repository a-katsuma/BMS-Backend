package com.example.app.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.SurveyMaster;
import com.example.app.mapper.SurveyMasterMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SurveyMasterServiceImpl implements SurveyMasterService {

	private final SurveyMasterMapper masterMapper;

	@Override
	public List<SurveyMaster> getAll() {
		return masterMapper.selectAll();
	}

	@Override
	public void add(SurveyMaster master) {
		master.setMasterId(null);
		validate(master);
		masterMapper.insert(master);
	}

	@Override
	public void update(SurveyMaster master) {
		SurveyMaster existing = masterMapper.selectById(master.getMasterId());
		if (existing == null) {
			throw new IllegalArgumentException("対象のマスタが見つかりません。");
		}
		master.setMasterType(existing.getMasterType()); // 種別は変更不可
		validate(master);
		masterMapper.update(master);
	}

	@Override
	public void delete(Integer masterId) {
		masterMapper.delete(masterId);
	}
	

	@Override
	public void move(Integer masterId, String direction) {
		SurveyMaster target = masterMapper.selectById(masterId);
		if (target == null) {
			throw new IllegalArgumentException("対象のマスタが見つかりません。");
		}
		int step;
		if ("up".equals(direction)) {
			step = -1;
		} else if ("down".equals(direction)) {
			step = 1;
		} else {
			throw new IllegalArgumentException("移動方向が不正です。");
		}

		List<SurveyMaster> list = masterMapper.selectByType(target.getMasterType());
		int index = -1;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getMasterId().equals(masterId)) {
				index = i;
				break;
			}
		}
		int swapIndex = index + step;
		if (index < 0 || swapIndex < 0 || swapIndex >= list.size()) {
			return; // 先頭・末尾なので動かさない
		}
		Collections.swap(list, index, swapIndex);

		// 1から振り直す（表示順が重なっていても正しく並ぶように）
		for (int i = 0; i < list.size(); i++) {
			SurveyMaster m = list.get(i);
			if (!Objects.equals(m.getDisplayOrder(), i + 1)) {
				masterMapper.updateDisplayOrder(m.getMasterId(), i + 1);
			}
		}
	}


	// 入力チェック（種別ごとに不要な項目はクリア）
	private void validate(SurveyMaster m) {
		String type = m.getMasterType();
		if (!List.of(SurveyMaster.TYPE_ITEM, SurveyMaster.TYPE_UNIT, SurveyMaster.TYPE_OTHER).contains(type)) {
			throw new IllegalArgumentException("種別が不正です。");
		}

		String name = (m.getName() == null) ? "" : m.getName().trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("名称は必須です。");
		}
		int maxLength = SurveyMaster.TYPE_UNIT.equals(type) ? 20 : 100;
		if (name.length() > maxLength) {
			throw new IllegalArgumentException("名称は" + maxLength + "文字以内で入力してください。");
		}
		if (masterMapper.countByTypeAndName(type, name, m.getMasterId()) > 0) {
			throw new IllegalArgumentException("「" + name + "」は既に登録されています。");
		}
		m.setName(name);

		if (!SurveyMaster.TYPE_ITEM.equals(type)) {
			m.setDefaultUnit(null);
		} else if (m.getDefaultUnit() != null) {
			m.setDefaultUnit(m.getDefaultUnit().trim().isEmpty() ? null : m.getDefaultUnit().trim());
		}
	}
}