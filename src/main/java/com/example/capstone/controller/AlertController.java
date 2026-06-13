package com.example.capstone.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
		log.info("[VLM 알림 API 호출] method=POST path=/api/alerts deviceId={} timestamp={} statusTextLength={} snapshot={}",
				request.deviceId(), request.timestamp(),
				request.statusText() == null ? 0 : request.statusText().length(),
				request.snapshot() == null || request.snapshot().isBlank() ? "없음" : "있음(length=" + request.snapshot().length() + ")");
		alertService.accept(request);
	}

	@GetMapping("/api/alerts/latest")
	public AlertResponse latestAlert(@RequestParam(required = false) String deviceId) {
		log.info("[VLM 최신 알림 API 호출] method=GET path=/api/alerts/latest deviceId={}",
				deviceId == null || deviceId.isBlank() ? "전체 최신" : deviceId);
		return alertService.latest(deviceId);
	}

	@GetMapping("/api/video-stream")
	public ResponseEntity<StreamingResponseBody> videoStream(
			@RequestParam(required = false) String roomId,
			@RequestParam(required = false) String cameraId) {
		log.info("[영상 스트림 API 호출] method=GET path=/api/video-stream roomId={} cameraId={}",
				roomId == null || roomId.isBlank() ? "미지정" : roomId,
				cameraId == null || cameraId.isBlank() ? "미지정" : cameraId);
		return ResponseEntity.ok()
				.contentType(MJPEG_MEDIA_TYPE)
				.body(videoStreamProxyService.proxy(roomId, cameraId));
	}

	@GetMapping(path = "/sse/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter alertStream() {
		log.info("[VLM 알림 SSE API 호출] method=GET path=/sse/alerts");
		return alertService.subscribe();
	}
}
