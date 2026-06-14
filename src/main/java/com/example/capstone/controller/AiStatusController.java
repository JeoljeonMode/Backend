package com.example.capstone.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.capstone.dto.AiStatusResponse;
import com.example.capstone.service.AiStatusService;
import com.example.capstone.service.VideoStreamProxyService;

@RestController
@RequestMapping("/api/ai")
public class AiStatusController {

	private static final Logger log = LoggerFactory.getLogger(AiStatusController.class);
	private static final MediaType MJPEG_MEDIA_TYPE =
			MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame");

	private final AiStatusService aiStatusService;
	private final VideoStreamProxyService videoStreamProxyService;
	private final String aiVideoFeedUrl;

	public AiStatusController(AiStatusService aiStatusService, VideoStreamProxyService videoStreamProxyService,
			@Value("${app.jetson.ai-base-url}") String aiBaseUrl) {
		this.aiStatusService = aiStatusService;
		this.videoStreamProxyService = videoStreamProxyService;
		this.aiVideoFeedUrl = aiBaseUrl + "/video_feed";
	}

	@GetMapping("/status")
	public AiStatusResponse status() {
		log.info("[AI 상태 조회 API 호출] method=GET path=/api/ai/status");
		return aiStatusService.fetchStatus();
	}

	@GetMapping("/video-stream")
	public ResponseEntity<StreamingResponseBody> videoStream() {
		log.info("[AI 영상 스트림 API 호출] method=GET path=/api/ai/video-stream");
		return ResponseEntity.ok()
				.contentType(MJPEG_MEDIA_TYPE)
				.body(videoStreamProxyService.proxyUrl(aiVideoFeedUrl, "jetson-ai"));
	}
}
