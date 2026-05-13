package com.example.capstone.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5173")
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(false);
		registry.addMapping("/sse/**")
				.allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5173")
				.allowedMethods("GET", "OPTIONS")
				.allowedHeaders("*");
	}
}
