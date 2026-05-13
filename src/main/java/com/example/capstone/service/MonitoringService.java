package com.example.capstone.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.capstone.domain.MonitoringEvent;
import com.example.capstone.domain.MonitoringEvent.DetectionBox;
import com.example.capstone.domain.RiskLevel;
import com.example.capstone.dto.AiEventRequest;
import com.example.capstone.dto.AiEventRequest.Box;
import com.example.capstone.dto.BedStatusResponse;
import com.example.capstone.dto.EventResponse;
import com.example.capstone.dto.StatusSummaryResponse;
import com.example.capstone.repository.EventStore;
import com.example.capstone.service.RiskAssessmentService.RiskAssessment;

@Service
public class MonitoringService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
	private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

	private final EventStore eventStore;
	private final RiskAssessmentService riskAssessmentService;
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final AtomicInteger demoStep = new AtomicInteger();

	public MonitoringService(EventStore eventStore, RiskAssessmentService riskAssessmentService) {
		this.eventStore = eventStore;
		this.riskAssessmentService = riskAssessmentService;
	}

	public EventResponse acceptEvent(AiEventRequest request) {
		RiskAssessment assessment = riskAssessmentService.assess(request);

		MonitoringEvent event = new MonitoringEvent();
		event.setOccurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt());
		event.setCameraId(valueOrDefault(request.cameraId(), "CAM-01"));
		event.setBedId(valueOrDefault(request.bedId(), "BED-01"));
		event.setPatientName(valueOrDefault(request.patientName(), "관리 환자"));
		event.setPatientPosition(valueOrDefault(request.patientPosition(), "center"));
		event.setPosture(valueOrDefault(request.posture(), "lying"));
		event.setGuardrailUp(Boolean.TRUE.equals(request.guardrailUp()));
		event.setCaregiverPresent(Boolean.TRUE.equals(request.caregiverPresent()));
		event.setRiskScore(assessment.score());
		event.setRiskLevel(assessment.level());
		event.setRiskFactors(assessment.factors());
		event.setSummary(assessment.summary());
		event.setFrameUrl(request.frameUrl());
		event.setRoi(toDetectionBox(request.roi()));
		event.setPatientBox(toDetectionBox(request.patientBox()));

		EventResponse response = EventResponse.from(eventStore.save(event));
		log.info("[이벤트 수신] bedId={} level={} score={} factors={}",
				response.bedId(), response.riskLevel(), response.riskScore(), response.riskFactors());
		broadcast(response);
		return response;
	}

	public EventResponse currentStatus() {
		return eventStore.findLatest()
				.map(EventResponse::from)
				.orElseGet(() -> acceptEvent(new AiEventRequest(
						Instant.now(), "CAM-01", "BED-01", "관리 환자", "center", "lying", true, true, null,
						new Box(18, 18, 62, 62), new Box(40, 36, 18, 28))));
	}

	public List<EventResponse> recentEvents(int limit) {
		return eventStore.findRecent(Math.max(1, Math.min(limit, 100))).stream()
				.map(EventResponse::from)
				.toList();
	}

	public List<EventResponse> searchEvents(String bedId, RiskLevel riskLevel, Boolean acknowledged, int limit) {
		return eventStore.findRecent(bedId, riskLevel, acknowledged, Math.max(1, Math.min(limit, 100))).stream()
				.map(EventResponse::from)
				.toList();
	}

	public List<BedStatusResponse> bedStatuses() {
		Map<String, EventResponse> latestByBed = recentEvents(100).stream()
				.collect(Collectors.toMap(
						EventResponse::bedId,
						event -> event,
						(first, second) -> first.occurredAt().isAfter(second.occurredAt()) ? first : second
				));
		return latestByBed.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new BedStatusResponse(entry.getKey(), entry.getValue()))
				.toList();
	}

	public EventResponse acknowledgeEvent(String eventId) {
		MonitoringEvent event = eventStore.findById(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
		event.setAcknowledged(true);
		event.setAcknowledgedAt(Instant.now());
		return EventResponse.from(eventStore.save(event));
	}

	public StatusSummaryResponse summary() {
		List<EventResponse> events = recentEvents(100);
		EventResponse latest = currentStatus();
		return new StatusSummaryResponse(
				events.size(),
				countByLevel(events, RiskLevel.DANGER),
				countByLevel(events, RiskLevel.CAUTION),
				countByLevel(events, RiskLevel.NORMAL),
				latest.riskScore(),
				latest.riskLabel(),
				latest.summary()
		);
	}

	public EventResponse generateDemoEvent() {
		int step = demoStep.getAndIncrement() % 4;
		return switch (step) {
			case 0 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "BED-01", "김환자",
					"center", "lying", true, true, null,
					new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
			case 1 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "BED-01", "김환자",
					"right_edge", "sitting", true, false, null,
					new Box(18, 18, 62, 62), new Box(58, 26, 18, 30)));
			case 2 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "BED-01", "김환자",
					"right_edge", "exit_attempt", false, false, null,
					new Box(18, 18, 62, 62), new Box(64, 24, 18, 34)));
			default -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "BED-01", "김환자",
					"left_edge", "sitting", false, true, null,
					new Box(18, 18, 62, 62), new Box(20, 27, 18, 30)));
		};
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		emitters.add(emitter);
		emitter.onCompletion(() -> {
			emitters.remove(emitter);
			log.info("[SSE] 클라이언트 연결 종료. 현재 구독자 수: {}", emitters.size());
		});
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(error -> emitters.remove(emitter));
		log.info("[SSE] 클라이언트 연결. 현재 구독자 수: {}", emitters.size());
		send(emitter, currentStatus());
		return emitter;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (eventStore.findLatest().isPresent()) {
			return;
		}
		log.info("[시작] 초기 시드 이벤트 3건 적재");
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(90), "CAM-01", "BED-01", "김환자",
				"center", "lying", true, true, null,
				new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(45), "CAM-01", "BED-01", "김환자",
				"right_edge", "sitting", true, false, null,
				new Box(18, 18, 62, 62), new Box(58, 26, 18, 30)));
		acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "BED-01", "김환자",
				"right_edge", "exit_attempt", false, false, null,
				new Box(18, 18, 62, 62), new Box(64, 24, 18, 34)));
	}

	private void broadcast(EventResponse response) {
		for (SseEmitter emitter : emitters) {
			send(emitter, response);
		}
	}

	private void send(SseEmitter emitter, EventResponse response) {
		try {
			emitter.send(SseEmitter.event().name("status").data(response));
		}
		catch (Exception ignored) {
			emitters.remove(emitter);
		}
	}

	private String valueOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private long countByLevel(List<EventResponse> events, RiskLevel level) {
		return events.stream().filter(event -> event.riskLevel() == level).count();
	}

	private DetectionBox toDetectionBox(Box box) {
		if (box == null) {
			return null;
		}
		return new DetectionBox(box.x(), box.y(), box.width(), box.height());
	}
}
