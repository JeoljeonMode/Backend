package com.example.capstone.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.capstone.repository.RoomStore;

@Service
public class VideoStreamProxyService {

	private static final Logger log = LoggerFactory.getLogger(VideoStreamProxyService.class);

	private final String upstreamUrl;
	private final int connectTimeoutMillis;
	private final int readTimeoutMillis;
	private final long retryDelayMillis;
	private final Map<String, String> cameraVideoFeedUrls;
	private final RoomStore roomStore;

	public VideoStreamProxyService(
			@Value("${app.jetson.video-feed-url}") String upstreamUrl,
			@Value("${app.jetson.connect-timeout-ms:3000}") int connectTimeoutMillis,
			@Value("${app.jetson.read-timeout-ms:0}") int readTimeoutMillis,
			@Value("${app.jetson.retry-delay-ms:1000}") long retryDelayMillis,
			Environment environment,
			RoomStore roomStore) {
		this.upstreamUrl = upstreamUrl;
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.readTimeoutMillis = readTimeoutMillis;
		this.retryDelayMillis = retryDelayMillis;
		this.roomStore = roomStore;
		this.cameraVideoFeedUrls = Binder.get(environment)
				.bind("app.jetson.video-feed-urls", Bindable.mapOf(String.class, String.class))
				.orElse(Map.of());
	}

	public StreamingResponseBody proxy() {
		return proxy(null, null);
	}

	public StreamingResponseBody proxy(String roomId, String cameraId) {
		String resolvedCameraId = resolveCameraId(roomId, cameraId);
		String resolvedUpstreamUrl = resolveUpstreamUrl(resolvedCameraId);
		log.info("[영상 스트림 준비] roomId={} 요청cameraId={} resolvedCameraId={} upstreamUrl={}",
				valueOrDefault(roomId, "미지정"), valueOrDefault(cameraId, "미지정"),
				valueOrDefault(resolvedCameraId, "미지정"), valueOrDefault(resolvedUpstreamUrl, "미설정"));
		return buildStreamingBody(resolvedUpstreamUrl, resolvedCameraId);
	}

	public StreamingResponseBody proxyUrl(String upstreamUrl, String label) {
		log.info("[영상 스트림 준비] label={} upstreamUrl={}", label, valueOrDefault(upstreamUrl, "미설정"));
		return buildStreamingBody(upstreamUrl, label);
	}

	private StreamingResponseBody buildStreamingBody(String resolvedUpstreamUrl, String resolvedCameraId) {
		return outputStream -> {
			while (!Thread.currentThread().isInterrupted()) {
				if (resolvedUpstreamUrl == null || resolvedUpstreamUrl.isBlank()) {
					log.warn("[영상 스트림 설정 없음] cameraId={} action=placeholder 전송",
							valueOrDefault(resolvedCameraId, "미지정"));
					if (!writePlaceholderFrame(outputStream, "Live camera not configured", resolvedCameraId)) {
						return;
					}
					sleepBeforeRetry();
					continue;
				}
				HttpURLConnection connection = null;
				try {
					log.info("[Jetson 영상 연결 시도] cameraId={} url={} connectTimeoutMs={} readTimeoutMs={}",
							valueOrDefault(resolvedCameraId, "미지정"), resolvedUpstreamUrl,
							connectTimeoutMillis, readTimeoutMillis);
					connection = openConnection(resolvedUpstreamUrl);
					int status = connection.getResponseCode();
					if (status < 200 || status >= 300) {
						log.warn("[Jetson 영상 응답 오류] cameraId={} status={} url={} action=placeholder 전송",
								valueOrDefault(resolvedCameraId, "미지정"), status, resolvedUpstreamUrl);
						if (!writePlaceholderFrame(outputStream, "Camera unavailable", resolvedCameraId)) {
							return;
						}
						sleepBeforeRetry();
						continue;
					}
					log.info("[Jetson 영상 연결 성공] cameraId={} status={} contentType={} url={}",
							valueOrDefault(resolvedCameraId, "미지정"), status, connection.getContentType(), resolvedUpstreamUrl);
					if (!copyStream(connection, outputStream)) {
						return;
					}
				}
				catch (IOException e) {
					log.warn("[Jetson 영상 연결 실패] cameraId={} url={} exception={} message={} action=placeholder 전송",
							valueOrDefault(resolvedCameraId, "미지정"), resolvedUpstreamUrl,
							e.getClass().getName(), e.getMessage());
					if (!writePlaceholderFrame(outputStream, "Camera unavailable", resolvedCameraId)) {
						return;
					}
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

	private HttpURLConnection openConnection(String videoFeedUrl) throws IOException {
		URL url = URI.create(videoFeedUrl).toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(connectTimeoutMillis);
		connection.setReadTimeout(readTimeoutMillis);
		connection.setRequestProperty("Accept", "multipart/x-mixed-replace,image/jpeg,*/*");
		return connection;
	}

	private String resolveCameraId(String roomId, String cameraId) {
		if (cameraId != null && !cameraId.isBlank()) {
			return cameraId;
		}
		if (roomId != null && !roomId.isBlank()) {
			return roomStore.findByRoomId(roomId)
					.map(room -> room.getCameraId())
					.orElse(null);
		}
		return null;
	}

	private String resolveUpstreamUrl(String cameraId) {
		if (cameraId != null && cameraVideoFeedUrls.containsKey(cameraId)) {
			return cameraVideoFeedUrls.get(cameraId);
		}
		return cameraId == null ? upstreamUrl : null;
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
					log.info("[영상 스트림 종료] reason=클라이언트 연결 종료");
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

	private boolean writePlaceholderFrame(OutputStream outputStream, String title, String cameraId) {
		try {
			byte[] image = createPlaceholderImage(title, cameraId);
			outputStream.write(("--frame\r\n"
					+ "Content-Type: image/jpeg\r\n"
					+ "Content-Length: " + image.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			outputStream.write(image);
			outputStream.write("\r\n".getBytes(StandardCharsets.US_ASCII));
			outputStream.flush();
			log.info("[영상 fallback 전송] title={} cameraId={} imageBytes={}",
					title, valueOrDefault(cameraId, "미지정"), image.length);
			return true;
		}
		catch (IOException clientDisconnected) {
			log.info("[영상 스트림 종료] reason=placeholder 전송 중 클라이언트 연결 종료");
			return false;
		}
	}

	private String valueOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private byte[] createPlaceholderImage(String title, String cameraId) throws IOException {
		BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(26, 31, 38));
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			graphics.setColor(new Color(78, 86, 96));
			graphics.fillRoundRect(330, 170, 300, 140, 24, 24);
			graphics.setColor(new Color(229, 235, 243));
			graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
			graphics.drawString(title, 300, 360);
			graphics.setColor(new Color(174, 184, 196));
			graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
			graphics.drawString(cameraId == null ? "Waiting for live stream" : "Camera: " + cameraId, 330, 405);
		}
		finally {
			graphics.dispose();
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", output);
		return output.toByteArray();
	}
}
