package com.example.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.Company;
import com.example.app.mapper.CompanyMapper;
import com.example.app.util.FormatUtils;

import lombok.RequiredArgsConstructor;

@Service
@Transactional // 途中で1個でも失敗したらロールバック
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

	private final CompanyMapper companyMapper;
	private final FormatUtils fmt; // 追加
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
		normalizeCompany(company); // 追加
		companyMapper.insert(company);
	}

	// ⑥編集
	@Override
	public void editCompany(Company company) {
		normalizeCompany(company); // 追加
		companyMapper.update(company);
	}

	// ⑦削除
	@Override
	public void deleteCompany(Integer id) {
		companyMapper.delete(id);
	}
}