# BMS System - Backend

BMS System の API（Spring Boot + MyBatis）です。
システム全体の説明・デモ・構成図は [bms-frontend の README](https://github.com/a-katsuma/bms-frontend) を見てください。

## 技術

Java 21 / Spring Boot 4.0 / MyBatis / MySQL 8.4 / Bean Validation / Spring Mail / BCrypt（spring-security-crypto）

## フォルダ構成

```
src/main/java/com/example/app/
├── config/                 # 設定（CORS など）
├── controller/             # 管理者用 API（/api/...）
│   └── contractee/         # 発注元用 API（/api/contractee/...）
├── domain/                 # テーブルに対応するクラス
├── dto/                    # 画面とやり取りするデータ
├── mapper/                 # MyBatis の Mapper（interface）
├── service/                # 業務処理
└── util/
src/main/resources/
├── mybatis/                # Mapper の SQL（XML）
├── application.properties  # 設定（本番は環境変数で上書き）
└── validation.properties   # 入力チェックのメッセージ
```

## API の決まり

- 管理者は `/api/...`、発注元は `/api/contractee/...`
- 作成は POST、更新は PUT、削除は DELETE、確定・受注などの操作は POST
- 成功は `{ message }`、エラーは `{ errorMessage }` を返す
- 認証はセッション。パスワードは BCrypt で変換して保存

## 起動

ローカルでの動かし方（DB の作成・バックエンド・フロントエンド）は、[bms-frontend の README の「ローカルで動かす」](https://github.com/a-katsuma/bms-frontend#ローカルで動かす) を見てください。

バックエンドだけを起動する場合は、MySQL を用意し、環境変数 `DB_PASSWORD`（必要なら `DB_URL`・`DB_USERNAME`）を設定して `./mvnw spring-boot:run`（Windows は `.\mvnw.cmd spring-boot:run`）を実行します。デモ用ログインを使うときは `DEMO_ENABLED=true` も設定します。

- **単体で起動**：MySQL を用意し、環境変数 `DB_URL`・`DB_USERNAME`・`DB_PASSWORD` を設定して `./mvnw spring-boot:run`

## 旧画面（Thymeleaf）のファイルについて

このシステムは、最初は Spring Boot + Thymeleaf で画面を作り、のちに React（bms-frontend）へ移行しました。
次のファイルは移行前の名残りで、**今の画面（React）では使っていません**。整理して削除する予定です。

- `src/main/resources/templates/`、`src/main/resources/static/`
- `pom.xml` の `spring-boot-starter-thymeleaf`
- `controller/contractee/ContracteeQuoteController.java`
