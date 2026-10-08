package com.example.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

	// CORS で許可する画面の URL（application.properties の app.cors.allowed-origins。カンマ区切りで複数指定できる）
	@Value("${app.cors.allowed-origins}")
	private String[] corsAllowedOrigins;

	// バリデーションメッセージの仕組みをシステム全体に有効化する設定
	@Override
	public Validator getValidator() {
		var validator = new LocalValidatorFactoryBean();
		validator.setValidationMessageSource(messageSource());
		return validator;
	}

	// 読み込むプロパティファイル（validation）の名前を指定する設定
	@Bean
	MessageSource messageSource() {
		var messageSource = new ResourceBundleMessageSource();

		messageSource.setBasename("validation");

		return messageSource;
	}

	// アップロードしたファイルへのアクセスを許可する設定
	// "/uploads/**" へのアクセスを、実行しているフォルダの中の "uploads/" に紐付ける（Docker では /app/uploads）
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:uploads/");
	}

	// 画面（React）からの CORS アクセスを許可する設定
	// 本番は画面と API を同じドメインで配信するので、CORS は開発環境（localhost:3000 → 8080）でだけ使われる
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(corsAllowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
