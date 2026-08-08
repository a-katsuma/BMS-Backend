package com.example.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Client;
import com.example.app.mapper.ClientMapper;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@Service
@Transactional // 途中で1個でも失敗したらロールバック
@RequiredArgsConstructor // finalがついたMapperを自動でドッキング
public class ClientServiceImpl implements ClientService {

	private final ClientMapper clientMapper;
	private final FormatUtils fmt; // コンストラクタ注入で利用

	private static final int NUM_PER_PAGE = 8;

	// 郵便・電話番号・読みカナの正規化ロジックを共通化（DRY原則）
	private void normalizeClient(Client client) {
		// 共通パーツ fmt.normalize を使って正規化
		client.setClientPhone(fmt.normalize(client.getClientPhone()));
		client.setClientPostalcode(fmt.normalize(client.getClientPostalcode()));
		client.setClientKana(fmt.kana(client.getClientKana()));
	}

	// 管理者用----------
	// ①全件取得(他ページ等で利用)
	@Override
	public List<Client> getAllClients() {
		List<Client> clients = clientMapper.selectAll();

		// 取得した全件に対して整形済みデータをセット
		for (Client c : clients) {
			if (c != null) {
				c.setFormattedClientPostalcode(fmt.postal(c.getClientPostalcode()));
				c.setFormattedClientPhone(fmt.format(c.getClientPhone()));
			}
		}
		return clients;
	}

	// ②１件取得(編集用)
	@Override
	public Client getClientById(Integer id) {
		Client c = clientMapper.selectById(id);
		// 1件だけの場合も整形
		if (c != null) {
			c.setFormattedClientPostalcode(fmt.postal(c.getClientPostalcode()));
			c.setFormattedClientPhone(fmt.format(c.getClientPhone()));
		}
		return c;
	}

	//	③ページング付き全件取得
	@Override
	public List<Client> getAllClients(int page) {
		int offset = (page - 1) * NUM_PER_PAGE;
		List<Client> clients = clientMapper.selectAllWithPage(offset, NUM_PER_PAGE);

		for (Client c : clients) {
			if (c != null) {
				c.setFormattedClientPostalcode(fmt.postal(c.getClientPostalcode()));
				c.setFormattedClientPhone(fmt.format(c.getClientPhone()));
			}
		}
		return clients;
	}

	@Override
	public int getTotalPagesAll() {
		long count = clientMapper.countAll();
		return (int) Math.ceil((double) count / NUM_PER_PAGE);
	}

	// ④カナ範囲指定ページング付き取得
	@Override
	public List<Client> getClientsByKana(String kana, int page) {
		// 入り口メソッド：変換処理を行ってから Range メソッドへ投げる
		String start = fmt.getKanaRangeStart(kana);
		String end = fmt.getKanaRangeEnd(kana);
		return getClientsByKanaRange(start, end, page);
	}

	@Override
	public int getTotalPagesByKana(String kana) {
		String start = fmt.getKanaRangeStart(kana);
		String end = fmt.getKanaRangeEnd(kana);
		return getTotalPagesByKanaRange(start, end);
	}

	@Override
	public List<Client> getClientsByKanaRange(String start, String end, int page) {
		int offset = (page - 1) * NUM_PER_PAGE;

		// ★ ここでMapperに正確な引数（4つ）を渡す
		List<Client> clients = clientMapper.selectByKanaWithPage(start, end, offset, NUM_PER_PAGE);

		for (Client c : clients) {
			if (c != null) {
				c.setFormattedClientPostalcode(fmt.postal(c.getClientPostalcode()));
				c.setFormattedClientPhone(fmt.format(c.getClientPhone()));
			}
		}
		return clients;
	}

	@Override
	public int getTotalPagesByKanaRange(String start, String end) {
		long count = clientMapper.countByKanaRange(start, end);
		return (int) Math.ceil((double) count / NUM_PER_PAGE);
	}

	// ⑤新規登録
	@Override
	public void addClient(Client client) {
		// 保存前に正規化！
		normalizeClient(client);
		clientMapper.insert(client);
	}

	// ⑥編集
	@Override
	public void editClient(Client client) {
		// 更新時も保存前に正規化！
		normalizeClient(client);
		clientMapper.update(client);
	}

	// ⑦削除
	@Override
	public void deleteClient(Integer id) {
		clientMapper.delete(id);
	}

	// 発注業者用----------

	// ③ページング付き全件取得
	@Override
	public List<Client> getClientsByCompanyId(Integer companyId, int page) {
		int offset = (page - 1) * NUM_PER_PAGE;
		// 必要であればMapperに selectByCompanyIdWithPage のようなページング用SQLを用意してください
		List<Client> clients = clientMapper.selectByCompanyIdWithPage(companyId, offset, NUM_PER_PAGE);

		// 整形処理
		for (Client c : clients) {
			if (c != null) {
				c.setFormattedClientPostalcode(fmt.postal(c.getClientPostalcode()));
				c.setFormattedClientPhone(fmt.format(c.getClientPhone()));
			}
		}
		return clients;
	}

	@Override
	public int getTotalPagesByCompanyId(Integer companyId) {
		long count = clientMapper.countByCompanyId(companyId);
		return (int) Math.ceil((double) count / NUM_PER_PAGE);

	}

	//	④カナ範囲指定ページング付き取得
	@Override
	public List<Client> getClientsByCompanyIdAndKana(Integer companyId, String start, String end, int page) {
		int offset = (page - 1) * NUM_PER_PAGE;
		List<Client> clients = clientMapper.selectByCompanyIdAndKanaWithPage(companyId, start, end, offset,
				NUM_PER_PAGE);

		// ★ここを確認！：このループ処理が抜けていると整形されません
		for (Client c : clients) {
			if (c != null) {
				c.setFormattedClientPostalcode(fmt.postal(c.getClientPostalcode()));
				c.setFormattedClientPhone(fmt.format(c.getClientPhone()));
			}
		}
		return clients;
	}

	@Override
	public int getTotalPagesByCompanyIdAndKana(Integer companyId, String start, String end) {
		long count = clientMapper.countByCompanyIdAndKana(companyId, start, end);
		// ページ数計算時も定数 NUM_PER_PAGE を使う
		return (int) Math.ceil((double) count / NUM_PER_PAGE);
	}

}