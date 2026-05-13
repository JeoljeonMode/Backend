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
		log.info("[AI 이벤트] bedId={} posture={} position={} guardrail={} caregiver={}",
				request.bedId(), request.posture(), request.patientPosition(),
				request.guardrailUp(), request.caregiverPresent());
		return monitoringService.acceptEvent(request);
	}
}
