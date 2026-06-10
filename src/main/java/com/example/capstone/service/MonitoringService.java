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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
import com.example.capstone.repository.PatientStore;
import com.example.capstone.service.RiskAssessmentService.RiskAssessment;

@Service
public class MonitoringService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
	private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

	private final EventStore eventStore;
	private final RiskAssessmentService riskAssessmentService;
	private final PatientStore patientStore;
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final AtomicInteger demoStep = new AtomicInteger();

	public MonitoringService(EventStore eventStore, RiskAssessmentService riskAssessmentService, PatientStore patientStore) {
		this.eventStore = eventStore;
		this.riskAssessmentService = riskAssessmentService;
		this.patientStore = patientStore;
	}

	public EventResponse acceptEvent(AiEventRequest request) {
		RiskAssessment assessment = riskAssessmentService.assess(request);

		String bedId = valueOrDefault(request.bedId(), "B-101");
		String patientNo = request.patientNo();
		if (patientNo == null || patientNo.isBlank()) {
			patientNo = lookupPatientNo(bedId);
		}

		MonitoringEvent event = new MonitoringEvent();
		event.setOccurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt());
		event.setCameraId(valueOrDefault(request.cameraId(), toCameraId(bedId)));
		event.setBedId(bedId);
		event.setPatientName(valueOrDefault(request.patientName(), "관리 환자"));
		event.setPatientNo(patientNo);
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
						Instant.now(), "CAM-01", "B-101", "관리 환자", null, "center", "lying", true, true, null,
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
		Map<String, EventResponse> latestByBed = eventStore.findRecent(500).stream()
				.map(EventResponse::from)
				.collect(Collectors.toMap(
						EventResponse::bedId,
						event -> event,
						(first, second) -> first.occurredAt().isAfter(second.occurredAt()) ? first : second
				));
		return latestByBed.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new BedStatusResponse(
						entry.getKey(),
						toRoomId(entry.getKey()),
						entry.getValue().cameraId(),
						entry.getValue()))
				.toList();
	}

	public EventResponse acknowledgeEvent(String eventId) {
		MonitoringEvent event = eventStore.findById(eventId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + eventId));
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
		int step = demoStep.getAndIncrement() % 6;
		return switch (step) {
			case 0 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "B-101", "김철수", "24-1011",
					"center", "lying", true, true, null,
					new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
			case 1 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "B-101", "김철수", "24-1011",
					"right_edge", "sitting", true, false, null,
					new Box(18, 18, 62, 62), new Box(58, 26, 18, 30)));
			case 2 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "B-101", "김철수", "24-1011",
					"right_edge", "exit_attempt", false, false, null,
					new Box(18, 18, 62, 62), new Box(64, 24, 18, 34)));
			case 3 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-01", "B-103", "박민준", "24-1013",
					"out_of_bed", "exit_attempt", false, false, null,
					new Box(18, 18, 62, 62), new Box(80, 30, 18, 34)));
			case 4 -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-02", "B-105", "정우성", "24-1015",
					"left_edge", "sitting", false, true, null,
					new Box(18, 18, 62, 62), new Box(20, 27, 18, 30)));
			default -> acceptEvent(new AiEventRequest(Instant.now(), "CAM-04", "B-201", "김유진", "24-2011",
					"center", "lying", true, true, null,
					new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
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
		log.info("[시작] 초기 시드 이벤트 6건 적재");
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(120), "CAM-01", "B-101", "김철수", "24-1011",
				"center", "lying", true, true, null,
				new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(90), "CAM-01", "B-102", "이영희", "24-1012",
				"right_edge", "sitting", true, false, null,
				new Box(18, 18, 62, 62), new Box(58, 26, 18, 30)));
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(60), "CAM-01", "B-103", "박민준", "24-1013",
				"right_edge", "exit_attempt", false, false, null,
				new Box(18, 18, 62, 62), new Box(64, 24, 18, 34)));
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(45), "CAM-02", "B-104", "최수진", "24-1014",
				"center", "lying", true, true, null,
				new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(30), "CAM-02", "B-105", "정우성", "24-1015",
				"left_edge", "sitting", false, true, null,
				new Box(18, 18, 62, 62), new Box(20, 27, 18, 30)));
		acceptEvent(new AiEventRequest(Instant.now().minusSeconds(15), "CAM-04", "B-201", "김유진", "24-2011",
				"center", "lying", true, true, null,
				new Box(18, 18, 62, 62), new Box(40, 36, 18, 28)));
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

	private String lookupPatientNo(String bedId) {
		return patientStore.findByActive(true).stream()
				.filter(p -> bedId != null && bedId.equals(p.getBedId()))
				.findFirst()
				.map(p -> p.getPatientNumber())
				.orElse(null);
	}

	private static String toRoomId(String bedId) {
		if (bedId == null) return "unknown";
		return switch (bedId) {
			case "B-101", "B-102", "B-103" -> "101호";
			case "B-104", "B-105", "B-106" -> "102호";
			case "B-107", "B-108", "B-109", "B-110" -> "103호";
			case "B-201", "B-202", "B-203" -> "201호";
			case "B-204", "B-205" -> "202호";
			default -> bedId;
		};
	}

	private static String toCameraId(String bedId) {
		if (bedId == null) return "CAM-01";
		return switch (bedId) {
			case "B-101", "B-102", "B-103" -> "CAM-01";
			case "B-104", "B-105", "B-106" -> "CAM-02";
			case "B-107", "B-108", "B-109", "B-110" -> "CAM-03";
			case "B-201", "B-202", "B-203" -> "CAM-04";
			case "B-204", "B-205" -> "CAM-05";
			default -> "CAM-01";
		};
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
