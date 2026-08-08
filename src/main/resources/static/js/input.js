/**
 * 顧客管理システム（BMS）用スクリプト
 * リアルタイム全角→半角自動変換処理
 */
document.addEventListener("DOMContentLoaded", function() {

	// 全角英数字・全角ハイフンを半角に強制置換する関数
	function convertToHalfWidth(str) {
		return str.replace(/[！-～]/g, function(s) {
			return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
		}).replace(/ー/g, "-").replace(/－/g, "-");
	}

	// 対象となる入力フィールド（電話番号クラス、郵便番号クラス）を取得
	const numericInputs = document.querySelectorAll(".js-input-numeric, .p-postal-code");

	numericInputs.forEach(input => {
		// 入力された瞬間にリアルタイムで全角を半角に変え、数字とハイフン以外を消去
		input.addEventListener("input", function() {
			let converted = convertToHalfWidth(this.value);
			this.value = converted.replace(/[^0-9-]/g, "");
		});

		// フォーカスが外れた（離脱した）瞬間の最終チェック
		input.addEventListener("blur", function() {
			let converted = convertToHalfWidth(this.value);
			this.value = converted.replace(/[^0-9-]/g, "");
		});
	});
});