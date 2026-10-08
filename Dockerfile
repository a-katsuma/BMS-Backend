# ---- ビルド：Maven で jar を作る ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# 依存ライブラリを先に取得する（pom.xml が変わらなければ、次回からキャッシュを使うので速い）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- 実行：JRE だけの小さいイメージ ----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Tokyo

# root ではなく専用のユーザーで動かす
RUN useradd --system --create-home app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app
USER app

COPY --from=build /src/target/*.jar app.jar

# アップロードしたファイルは /app/uploads（compose でボリュームをつなぐ）
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
