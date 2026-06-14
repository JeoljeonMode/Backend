package com.example.capstone.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.capstone.dto.AiStatusResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiStatusService {

	private static final Logger log = LoggerFactory.getLogger(AiStatusService.class);

	private final String vlmTextUrl;
	private final int connectTimeoutMillis;
	private final int readTimeoutMillis;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AiStatusService(
			@Value("${app.jetson.ai-base-url}") String aiBaseUrl,
			@Value("${app.jetson.connect-timeout-ms:3000}") int connectTimeoutMillis,
			@Value("${app.jetson.ai-status-timeout-ms:5000}") int readTimeoutMillis) {
		this.vlmTextUrl = aiBaseUrl + "/vlm_text";
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public AiStatusResponse fetchStatus() {
		log.info("[AI 상태 조회 시작] url={}", vlmTextUrl);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) URI.create(vlmTextUrl).toURL().openConnection();
			connection.setConnectTimeout(connectTimeoutMillis);
			connection.setReadTimeout(readTimeoutMillis);
			connection.setRequestProperty("Accept", "application/json");

			int status = connection.getResponseCode();
			if (status < 200 || status >= 300) {
				log.warn("[AI 상태 조회 실패] url={} status={}", vlmTextUrl, status);
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
			}

			JsonNode body;
			try (InputStream inputStream = connection.getInputStream()) {
				body = objectMapper.readTree(inputStream);
			}
			String text = body.path("text").asText("");
			long timestamp = Instant.now().getEpochSecond();
			log.info("[AI 상태 조회 완료] url={} textLength={} timestamp={}", vlmTextUrl, text.length(), timestamp);
			return new AiStatusResponse(text, timestamp);
		}
		catch (IOException e) {
			log.warn("[AI 상태 조회 실패] url={} exception={} message={}", vlmTextUrl, e.getClass().getName(), e.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
