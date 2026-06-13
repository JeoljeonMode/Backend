package com.example.capstone.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class VideoStreamProxyService {

	private static final Logger log = LoggerFactory.getLogger(VideoStreamProxyService.class);

	private final String upstreamUrl;
	private final int connectTimeoutMillis;
	private final int readTimeoutMillis;
	private final long retryDelayMillis;

	public VideoStreamProxyService(
			@Value("${app.jetson.video-feed-url}") String upstreamUrl,
			@Value("${app.jetson.connect-timeout-ms:3000}") int connectTimeoutMillis,
			@Value("${app.jetson.read-timeout-ms:0}") int readTimeoutMillis,
			@Value("${app.jetson.retry-delay-ms:1000}") long retryDelayMillis) {
		this.upstreamUrl = upstreamUrl;
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.readTimeoutMillis = readTimeoutMillis;
		this.retryDelayMillis = retryDelayMillis;
	}

	public StreamingResponseBody proxy() {
		return outputStream -> {
			while (!Thread.currentThread().isInterrupted()) {
				HttpURLConnection connection = null;
				try {
					connection = openConnection();
					int status = connection.getResponseCode();
					if (status < 200 || status >= 300) {
						log.warn("[영상 스트림] Jetson 응답 오류 status={} url={}", status, upstreamUrl);
						sleepBeforeRetry();
						continue;
					}
					log.info("[영상 스트림] Jetson 연결 성공 url={}", upstreamUrl);
					if (!copyStream(connection, outputStream)) {
						return;
					}
				}
				catch (IOException e) {
					log.warn("[영상 스트림] Jetson 연결/읽기 실패 url={} message={}", upstreamUrl, e.getMessage());
					sleepBeforeRetry();
				}
				finally {
					if (connection != null) {
						connection.disconnect();
					}
				}
			}
		};
	}

	private HttpURLConnection openConnection() throws IOException {
		URL url = URI.create(upstreamUrl).toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(connectTimeoutMillis);
		connection.setReadTimeout(readTimeoutMillis);
		connection.setRequestProperty("Accept", "multipart/x-mixed-replace,image/jpeg,*/*");
		return connection;
	}

	private boolean copyStream(HttpURLConnection connection, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[8192];
		try (InputStream inputStream = connection.getInputStream()) {
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				try {
					outputStream.write(buffer, 0, bytesRead);
					outputStream.flush();
				}
				catch (IOException clientDisconnected) {
					log.info("[영상 스트림] 클라이언트 연결 종료");
					return false;
				}
			}
		}
		return true;
	}

	private void sleepBeforeRetry() {
		try {
			Thread.sleep(retryDelayMillis);
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
