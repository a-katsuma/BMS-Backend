package com.example.app.service;

import java.util.List;

import com.example.app.domain.Company;
import com.example.app.dto.CompanyRegisterRequest;

public interface CompanyService {

	//	①全件取得(他ページ等で利用)
	List<Company> getAllCompanys();

	

	// ②１件取得(編集用)
	Company getCompanyById(Integer id);

	// ③ページング付き全件取得
	List<Company> getAllCompanies(int page);

	int getTotalPagesAll();
	
	// ④カナ範囲指定ページング付き取得
	// 入口（玄関）
	List<Company> getCompanysByKana(String kana, int page);
	
	int getTotalPagesByKana(String kana);
	
	// 実務（本体)
	public List<Company> getCompanysByKanaRange(String start, String end, int page);

	public int getTotalPagesByKanaRange(String start, String end);

	
	// ⑤新規登録
	void addCompany(Company company);

	// ⑥編集
	void editCompany(Company company);

	// ⑦削除
	void deleteCompany(Integer id);
	
	// マスターユーザー登録
	Company addCompanyWithMaster(CompanyRegisterRequest req);


}
