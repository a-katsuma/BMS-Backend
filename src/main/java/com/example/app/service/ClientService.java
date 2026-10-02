package com.example.app.service;

import java.util.List;

import com.example.app.domain.Client;

public interface ClientService {

	// 管理者用

	//	①全件取得(他ページ等で利用)
	List<Client> getAllClients();

	// ②１件取得(編集用)
	Client getClientById(Integer id);

	//	③ページング付き全件取得
	List<Client> getAllClients(int page);

	int getTotalPagesAll();

	// ④カナ範囲指定ページング付き取得

	//	入口（玄関）
	List<Client> getClientsByKana(String kana, int page);

	int getTotalPagesByKana(String kana);

	//	実務（本体）
	List<Client> getClientsByKanaRange(String start, String end, int page);

	int getTotalPagesByKanaRange(String start, String end);

	// ⑤新規登録
	void addClient(Client client);

	// ⑥編集
	void editClient(Client client);

	// ⑦削除
	void deleteClient(Integer id);

	// 発注業者用
	
	// ②１件取得
	Client getClientByIdAndCompanyId(Integer clientId, Integer companyId);

	// ③ページング付き全件取得
	List<Client> getClientsByCompanyId(Integer companyId, int page);

	int getTotalPagesByCompanyId(Integer companyId);

	//	④カナ範囲指定ページング付き取得
	List<Client> getClientsByCompanyIdAndKana(Integer companyId, String start, String end, int page);

	int getTotalPagesByCompanyIdAndKana(Integer companyId, String start, String end);

}