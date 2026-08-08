package com.example.app.util;

import org.springframework.stereotype.Component;

@Component("fmt")
public class FormatUtils {

	// 1. 保存用：数値化して半角にする
	public String normalize(String val) {
		if (val == null)
			return null;
		String result = val.replace("０", "0").replace("１", "1").replace("２", "2")
				.replace("３", "3").replace("４", "4").replace("５", "5")
				.replace("６", "6").replace("７", "7").replace("８", "8")
				.replace("９", "9");
		return result.replaceAll("[^0-9]", "");
	}

	// 2. 表示用：電話番号の整形
	public String format(String phone) {
		if (phone == null || phone.isEmpty())
			return "";
		String p = normalize(phone);
		int len = p.length();

		// 携帯・IP (11桁) -> 3-4-4
		if (len == 11 && (p.startsWith("090") || p.startsWith("080") || p.startsWith("070") || p.startsWith("050"))) {
			return p.substring(0, 3) + "-" + p.substring(3, 7) + "-" + p.substring(7);
		}
		// 10桁の固定電話
		if (len == 10) {
			if (p.startsWith("03") || p.startsWith("06")) {
				return p.substring(0, 2) + "-" + p.substring(2, 6) + "-" + p.substring(6);
			}
			if (p.startsWith("0193")) { // 4桁市外局番の特別処理
				return p.substring(0, 4) + "-" + p.substring(4, 6) + "-" + p.substring(6);
			}
			return p.substring(0, 3) + "-" + p.substring(3, 6) + "-" + p.substring(6);
		}
		// 11桁の固定電話 (4桁市外局番など)
		if (len == 11) {
			return p.substring(0, 4) + "-" + p.substring(4, 7) + "-" + p.substring(7);
		}
		return p;
	}

	// 3. 表示用：郵便番号の整形
	public String postal(String code) {
		if (code == null || code.isEmpty())
			return "";
		String c = normalize(code);
		if (c.length() == 7) {
			return c.substring(0, 3) + "-" + c.substring(3);
		}
		return c;
	}

	// 読み仮名用の正規化（ひらがな→カタカナ変換）
	public String kana(String kana) {
		if (kana == null || kana.isEmpty()) {
			return "";
		}

		// 1. 全角ひらがなを全角カタカナに変換
		StringBuilder sb = new StringBuilder();
		for (char c : kana.toCharArray()) {
			if (c >= 'ぁ' && c <= 'ん') {
				sb.append((char) (c + ('ァ' - 'ぁ')));
			} else {
				sb.append(c);
			}
		}

		// 2. カタカナ以外の不要な文字（スペースなど）を除去したい場合はここで行う
		// 例: 全角スペースを削除
		return sb.toString().replace(" ", "").replace(" ", "");
	}

	// 五十音の「行」を扱うための補助メソッド
	public String getKanaRangeStart(String row) {
		switch (row) {
		case "あ行":
			return "ア";
		case "か行":
			return "カ";
		case "さ行":
			return "サ";
		case "た行":
			return "タ";
		case "な行":
			return "ナ";
		case "は行":
			return "ハ";
		case "ま行":
			return "マ";
		case "や行":
			return "ヤ";
		case "ら行":
			return "ラ";
		case "わ行":
			return "ワ";
		default:
			return "";
		}
	}

	//行の「次の行の開始文字（＝終了境界）」を返す
	public String getKanaRangeEnd(String row) {
		switch (row) {
		case "あ行":
			return "カ";
		case "か行":
			return "サ";
		case "さ行":
			return "タ";
		case "た行":
			return "ナ";
		case "な行":
			return "ハ";
		case "は行":
			return "マ";
		case "ま行":
			return "ヤ";
		case "や行":
			return "ラ";
		case "ら行":
			return "ワ";
		case "わ行":
			return null; // 「ワ」以降すべて
		default:
			return null;
		}
	}
}
