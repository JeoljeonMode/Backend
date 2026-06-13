package com.example.capstone.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.capstone.dto.AiEventRequest;
import com.example.capstone.dto.EventResponse;
import com.example.capstone.service.MonitoringService;

@RestController
@RequestMapping("/api/ai")
public class AiEventController {

	private static final Logger log = LoggerFactory.getLogger(AiEventController.class);

	private final MonitoringService monitoringService;

	public AiEventController(MonitoringService monitoringService) {
		this.monitoringService = monitoringService;
	}

	@PostMapping("/events")
	@ResponseStatus(HttpStatus.CREATED)
	public EventResponse receiveEvent(@RequestBody AiEventRequest request) {
		log.info("[AI 이벤트 수신 시작] path=/api/ai/events bedId={} cameraId={} patientNo={} posture={} position={} guardrailUp={} caregiverPresent={}",
				request.bedId(), request.cameraId(), request.patientNo(), request.posture(), request.patientPosition(),
				request.guardrailUp(), request.caregiverPresent());
		EventResponse response = monitoringService.acceptEvent(request);
		log.info("[AI 이벤트 수신 완료] bedId={} eventId={} riskLevel={} riskScore={} summary={}",
				response.bedId(), response.id(), response.riskLevel(), response.riskScore(), response.summary());
		return response;
	}
}
