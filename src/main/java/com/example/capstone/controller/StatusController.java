package com.example.capstone.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.capstone.domain.RiskLevel;
import com.example.capstone.dto.BedStatusResponse;
import com.example.capstone.dto.EventResponse;
import com.example.capstone.dto.StatusSummaryResponse;
import com.example.capstone.service.MonitoringService;

@RestController
public class StatusController {

	private static final Logger log = LoggerFactory.getLogger(StatusController.class);

	private final MonitoringService monitoringService;

	public StatusController(MonitoringService monitoringService) {
		this.monitoringService = monitoringService;
	}

	@GetMapping("/api/status/current")
	public EventResponse currentStatus() {
		log.info("[GET] /api/status/current");
		return monitoringService.currentStatus();
	}

	@GetMapping("/api/events")
	public List<EventResponse> events(
			@RequestParam(required = false) String bedId,
			@RequestParam(required = false) RiskLevel riskLevel,
			@RequestParam(required = false) Boolean acknowledged,
			@RequestParam(defaultValue = "20") int limit) {
		log.info("[GET] /api/events bedId={} riskLevel={} limit={}", bedId, riskLevel, limit);
		return monitoringService.searchEvents(bedId, riskLevel, acknowledged, limit);
	}

	@GetMapping("/api/beds")
	public List<BedStatusResponse> bedStatuses() {
		return monitoringService.bedStatuses();
	}

	@PostMapping("/api/events/{eventId}/ack")
	public EventResponse acknowledge(@PathVariable String eventId) {
		log.info("[POST] /api/events/{}/ack", eventId);
		return monitoringService.acknowledgeEvent(eventId);
	}

	@GetMapping("/api/status/summary")
	public StatusSummaryResponse summary() {
		return monitoringService.summary();
	}

	@PostMapping("/api/demo/events")
	public EventResponse demoEvent() {
		log.info("[POST] /api/demo/events (데모 이벤트 생성)");
		return monitoringService.generateDemoEvent();
	}

	@GetMapping(path = "/sse/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter statusStream() {
		log.info("[GET] /sse/status (SSE 구독 요청)");
		return monitoringService.subscribe();
	}
}
