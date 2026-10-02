package com.example.app.config;

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
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// "/files/**" へのアクセスを、実際のフォルダ"/uploads/" に紐付ける
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:uploads/");
	}
	
	 // React (Vite) からのCORSアクセスを許可する設定
  @Override
  public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/api/**")
          .allowedOrigins("http://localhost:3000")
          .allowedMethods("GET", "POST", "PUT", "DELETE","PATCH")
          .allowedHeaders("*")
          .allowCredentials(true);
  }
}
