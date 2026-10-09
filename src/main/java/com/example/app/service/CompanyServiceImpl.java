package com.example.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Company;
import com.example.app.domain.User;
import com.example.app.dto.CompanyRegisterRequest;
import com.example.app.mapper.CompanyMapper;
import com.example.app.mapper.UserMapper;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@Service
@Transactional // 途中で1個でも失敗したらロールバック
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final CompanyMapper companyMapper;
	private final FormatUtils fmt;
	private final UserService userService; // 代表ユーザーへの招待（パスワード設定のリンク）
	private static final int NUM_PER_PAGE = 8;

	private void normalizeCompany(Company company) {
		company.setCompanyPhone(fmt.normalize(company.getCompanyPhone()));
		company.setCompanyPostalcode(fmt.normalize(company.getCompanyPostalcode()));
		company.setCompanyKana(fmt.kana(company.getCompanyKana()));
	}

	//	①全件取得(他ページ等で利用)
	@Override
	public List<Company> getAllCompanys() {
		List<Company> companies = companyMapper.selectAll();
		for (Company c : companies) {
			if (c != null) {
				c.setFormattedCompanyPostalcode(fmt.postal(c.getCompanyPostalcode()));
				c.setFormattedCompanyPhone(fmt.format(c.getCompanyPhone()));
			}
		}
		return companies;
	}

	//	②１件取得(編集用)
	@Override
	public Company getCompanyById(Integer id) {
		Company c = companyMapper.selectById(id);
		if (c != null) {
			c.setFormattedCompanyPostalcode(fmt.postal(c.getCompanyPostalcode()));
			c.setFormattedCompanyPhone(fmt.format(c.getCompanyPhone()));
		}
		return c;
	}

	//	③ページング付き全件取得
	@Override
	public List<Company> getAllCompanies(int page) {
		int offset = (page - 1) * NUM_PER_PAGE;
		List<Company> list = companyMapper.selectAllWithPage(offset, NUM_PER_PAGE);

		// ★重要：ここでも整形処理（ループ）を忘れないでください！
		for (Company c : list) {
			if (c != null) {
				c.setFormattedCompanyPostalcode(fmt.postal(c.getCompanyPostalcode()));
				c.setFormattedCompanyPhone(fmt.format(c.getCompanyPhone()));
			}
		}
		return list;
	}

	@Override
	public int getTotalPagesAll() {
		return (int) Math.ceil((double) companyMapper.countAll() / NUM_PER_PAGE);
	}

	// ④カナ範囲指定ページング付き取得
	@Override
	public List<Company> getCompanysByKana(String kana, int page) {
		// カナから開始文字・終了文字を取得して範囲検索メソッドを呼ぶ
		String start = fmt.getKanaRangeStart(kana);
		String end = fmt.getKanaRangeEnd(kana);
		return getCompanysByKanaRange(start, end, page);
	}

	@Override
	public int getTotalPagesByKana(String kana) {
		String start = fmt.getKanaRangeStart(kana);
		String end = fmt.getKanaRangeEnd(kana);
		return getTotalPagesByKanaRange(start, end);
	}

	@Override
	public List<Company> getCompanysByKanaRange(String start, String end, int page) {
		int limit = 8; // 8件/1P
		int offset = (page - 1) * limit;

		// 名前を Mapper で定義したものと合わせる
		List<Company> companies = companyMapper.selectByKanaWithPage(start, end, offset, limit);

		// 整形処理
		for (Company c : companies) {
			if (c != null) {
				c.setFormattedCompanyPostalcode(fmt.postal(c.getCompanyPostalcode()));
				c.setFormattedCompanyPhone(fmt.format(c.getCompanyPhone()));
			}
		}
		return companies;
	}

	@Override
	public int getTotalPagesByKanaRange(String start, String end) {
		long count = companyMapper.countByKanaRange(start, end);
		return (int) Math.ceil((double) count / NUM_PER_PAGE);
	}

	// ⑤新規登録
	@Override
	public void addCompany(Company company) {
		normalizeCompany(company);
		companyMapper.insert(company);
	}

	// ⑥編集
	@Override
	public void editCompany(Company company) {
		normalizeCompany(company);
		companyMapper.update(company);
	}

	// ⑦削除
	@Override
	public void deleteCompany(Integer id) {
		if (companyMapper.countAllProjects(id) > 0) {
			throw new IllegalStateException("案件（削除済みを含む）が登録されているため、削除できません。");
		}
		// 1. 先に紐づくユーザーを削除
		userMapper.deleteByCompanyId(id);
		// 2. 業者本体を削除
		companyMapper.delete(id);
	}


	// 業者の登録と、代表ユーザーの作成（パスワードは、招待のリンクから本人が決める）
	@Override
	public Company addCompanyWithMaster(CompanyRegisterRequest req) {
		// 0. 代表のメールアドレス（＝ログインID）が、ほかのユーザーと重ならないか（一般ユーザーの追加と同じ確認）
		if (userMapper.selectByLoginId(req.getMasterEmail()) != null) {
			throw new IllegalStateException("そのメールアドレスは既に使用されています");
		}
		// 1. 業者エンティティ作成と正規化
		Company company = new Company();
		company.setCompanyName(req.getCompanyName());
		company.setCompanyKana(req.getCompanyKana());
		company.setCompanyPostalcode(req.getCompanyPostalcode());
		company.setCompanyAddress(req.getCompanyAddress());
		company.setCompanyPhone(req.getCompanyPhone());

		normalizeCompany(company);
		companyMapper.insert(company); // useGeneratedKeys により companyId が自動セット

		// 2. 代表ユーザー作成（本人がリンクから設定するまでは、誰も知らないランダムなパスワードにしておく）
		User master = new User();
		master.setLoginId(req.getMasterEmail());
		master.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
		master.setName(req.getMasterName());
		master.setCompanyId(company.getCompanyId());
		master.setRoleFlag(2); // 2: 業者マスター
		master.setEmail(req.getMasterEmail());
		master.setIsActive(1); // 1: 有効

		userMapper.insert(master);

		// 3. 招待（パスワード設定のリンク）をメールで送る
		userService.sendInvitation(master);

		// 4. 生成された companyId を持つオブジェクトを返却
		return company;
	}
}
