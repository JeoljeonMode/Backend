package com.example.capstone.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.capstone.dto.AlertRequest;
import com.example.capstone.dto.AlertResponse;
import com.example.capstone.service.AlertService;
import com.example.capstone.service.VideoStreamProxyService;

@RestController
public class AlertController {

	private static final Logger log = LoggerFactory.getLogger(AlertController.class);
	private static final MediaType MJPEG_MEDIA_TYPE =
			MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame");

	private final AlertService alertService;
	private final VideoStreamProxyService videoStreamProxyService;

	public AlertController(AlertService alertService, VideoStreamProxyService videoStreamProxyService) {
		this.alertService = alertService;
		this.videoStreamProxyService = videoStreamProxyService;
	}

	@PostMapping("/api/alerts")
	@ResponseStatus(HttpStatus.OK)
	public void receiveAlert(@RequestBody AlertRequest request) {
		alertService.accept(request);
	}

	@GetMapping("/api/alerts/latest")
	public AlertResponse latestAlert() {
		return alertService.latest();
	}

	@GetMapping("/api/video-stream")
	public ResponseEntity<StreamingResponseBody> videoStream() {
		log.info("[GET] /api/video-stream");
		return ResponseEntity.ok()
				.contentType(MJPEG_MEDIA_TYPE)
				.body(videoStreamProxyService.proxy());
	}

	@GetMapping(path = "/sse/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter alertStream() {
		log.info("[GET] /sse/alerts (VLM 알림 SSE 구독 요청)");
		return alertService.subscribe();
	}
}
