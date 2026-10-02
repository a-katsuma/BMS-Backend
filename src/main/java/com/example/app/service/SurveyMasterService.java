package com.example.app.service;

import java.util.List;

import com.example.app.domain.SurveyMaster;

public interface SurveyMasterService {

	List<SurveyMaster> getAll();

	void add(SurveyMaster master);

	void update(SurveyMaster master);

	void delete(Integer masterId);
	

	// 並び替え（direction = up / down）
	void move(Integer masterId, String direction);

}
