package com.example.capstone.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.domain.MonitoringEvent;
import com.example.capstone.domain.MonitoringEvent.DetectionBox;
import com.example.capstone.domain.RiskLevel;
import com.example.capstone.dto.AiEventRequest;
import com.example.capstone.dto.AiEventRequest.Box;
import com.example.capstone.dto.BedStatusResponse;
import com.example.capstone.dto.EventResponse;
import com.example.capstone.dto.StatusSummaryResponse;
import com.example.capstone.repository.BedConfigStore;
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
	private final BedConfigStore bedConfigStore;
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final AtomicInteger demoStep = new AtomicInteger();

	public MonitoringService(EventStore eventStore, RiskAssessmentService riskAssessmentService,
			PatientStore patientStore, BedConfigStore bedConfigStore) {
		this.eventStore = eventStore;
		this.riskAssessmentService = riskAssessmentService;
		this.patientStore = patientStore;
		this.bedConfigStore = bedConfigStore;
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
		Map<String, BedConfig> bedConfigsByBedId = bedConfigStore.findAll().stream()
				.collect(Collectors.toMap(BedConfig::getBedId, config -> config, (first, second) -> first));

		// findRecent() is ordered by occurredAt DESC, id DESC, so the first event seen per bed is the latest one.
		Map<String, EventResponse> latestByBed = eventStore.findRecent(500).stream()
				.map(EventResponse::from)
				.collect(Collectors.toMap(
						EventResponse::bedId,
						event -> event,
						(first, second) -> first
				));

		return latestByBed.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> {
					BedConfig config = bedConfigsByBedId.get(entry.getKey());
					String roomId = config != null && config.getRoomId() != null
							? config.getRoomId() : toRoomId(entry.getKey());
					String cameraId = config != null && config.getCameraId() != null
							? config.getCameraId() : entry.getValue().cameraId();
					return new BedStatusResponse(entry.getKey(), roomId, cameraId, entry.getValue());
				})
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
		Instant t0 = Instant.now();
		for (SeedEvent seed : SEED_EVENTS) {
			upsertSeedEvent(seed, t0);
		}
		log.info("[시작] 시드 이벤트 {}건 upsert 완료 (T0={})", SEED_EVENTS.size(), t0);
	}

	private void upsertSeedEvent(SeedEvent seed, Instant t0) {
		BedInfo info = BED_INFO.get(seed.bedId());
		AiEventRequest request = new AiEventRequest(
				t0.minusMillis(seed.offsetMillis()),
				toCameraId(seed.bedId()),
				seed.bedId(),
				info.patientName(),
				info.patientNo(),
				seed.position(),
				seed.posture(),
				seed.guardrailUp(),
				seed.caregiverPresent(),
				null, null, null);
		RiskAssessment assessment = riskAssessmentService.assess(request);

		MonitoringEvent event = eventStore.findById(seed.id()).orElseGet(MonitoringEvent::new);
		event.setId(seed.id());
		event.setOccurredAt(request.occurredAt());
		event.setCameraId(request.cameraId());
		event.setBedId(request.bedId());
		event.setPatientName(request.patientName());
		event.setPatientNo(request.patientNo());
		event.setPatientPosition(request.patientPosition());
		event.setPosture(request.posture());
		event.setGuardrailUp(Boolean.TRUE.equals(request.guardrailUp()));
		event.setCaregiverPresent(Boolean.TRUE.equals(request.caregiverPresent()));
		event.setRiskScore(assessment.score());
		event.setRiskLevel(assessment.level());
		event.setRiskFactors(assessment.factors());
		event.setSummary(assessment.summary());
		eventStore.save(event);
	}

	private record BedInfo(String patientName, String patientNo) {
	}

	private record SeedEvent(String id, String bedId, long offsetMillis, String position, String posture,
			boolean guardrailUp, boolean caregiverPresent) {
	}

	private static final Map<String, BedInfo> BED_INFO = Map.ofEntries(
			Map.entry("B-101", new BedInfo("김철수", "24-1011")),
			Map.entry("B-102", new BedInfo("박영호", "24-1012")),
			Map.entry("B-103", new BedInfo("이민준", "24-1013")),
			Map.entry("B-104", new BedInfo("최진혁", "24-1021")),
			Map.entry("B-105", new BedInfo("정우성", "24-1022")),
			Map.entry("B-106", new BedInfo("윤기준", "24-1023")),
			Map.entry("B-107", new BedInfo("강민호", "24-1031")),
			Map.entry("B-108", new BedInfo("한동훈", "24-1032")),
			Map.entry("B-109", new BedInfo("조재원", "24-1033")),
			Map.entry("B-110", new BedInfo("임성규", "24-1034")),
			Map.entry("B-201", new BedInfo("김지연", "24-2011")),
			Map.entry("B-202", new BedInfo("박서현", "24-2012")),
			Map.entry("B-203", new BedInfo("이수현", "24-2013")),
			Map.entry("B-204", new BedInfo("최민서", "24-2021")),
			Map.entry("B-205", new BedInfo("정지현", "24-2022")));

	// docs/backend-seed-data-spec.md §5 병상 현재 상태 (15건)
	private static final List<SeedEvent> CURRENT_STATUS_SEED_EVENTS = List.of(
			new SeedEvent("e-101a", "B-101", 0L, "center", "sitting", true, false),
			new SeedEvent("e-101b", "B-102", 180_000L, "center", "lying", true, false),
			new SeedEvent("e-101c", "B-103", 360_000L, "center", "lying", true, true),
			new SeedEvent("e-102a", "B-104", 540_000L, "center", "lying", true, true),
			new SeedEvent("e-102b", "B-105", 720_000L, "center", "sitting", true, false),
			new SeedEvent("e-102c", "B-106", 900_000L, "center", "lying", true, false),
			new SeedEvent("e-103a", "B-107", 1_080_000L, "center", "lying", true, false),
			new SeedEvent("e-103b", "B-108", 1_260_000L, "center", "lying", true, true),
			new SeedEvent("e-103c", "B-109", 1_440_000L, "center", "lying", true, false),
			new SeedEvent("e-103d", "B-110", 1_620_000L, "right_edge", "exit_attempt", false, false),
			new SeedEvent("e-201a", "B-201", 1_800_000L, "center", "lying", true, true),
			new SeedEvent("e-201b", "B-202", 1_980_000L, "center", "lying", true, false),
			new SeedEvent("e-201c", "B-203", 2_160_000L, "center", "lying", true, true),
			new SeedEvent("e-202a", "B-204", 2_340_000L, "center", "lying", true, true),
			new SeedEvent("e-202b", "B-205", 2_520_000L, "out_of_bed", "exit_attempt", false, false));

	// docs/backend-seed-data-spec.md §6 이벤트 로그 (19건)
	private static final List<SeedEvent> EVENT_LOG_SEED_EVENTS = List.of(
			new SeedEvent("evt-B-205-r1", "B-205", 0L, "out_of_bed", "exit_attempt", false, false),
			new SeedEvent("evt-B-110-r2", "B-110", 300_000L, "right_edge", "exit_attempt", false, false),
			new SeedEvent("evt-B-101-r3", "B-101", 720_000L, "center", "sitting", true, false),
			new SeedEvent("evt-B-105-r4", "B-105", 1_080_000L, "center", "sitting", true, false),
			new SeedEvent("evt-B-102-r5", "B-102", 1_500_000L, "center", "lying", true, false),
			new SeedEvent("evt-B-204-r6", "B-204", 1_920_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-201-r7", "B-201", 2_400_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-107-r8", "B-107", 2_880_000L, "center", "lying", true, false),
			new SeedEvent("evt-B-205-h1", "B-205", 4_200_000L, "left_edge", "sitting", true, false),
			new SeedEvent("evt-B-205-h2", "B-205", 7_200_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-110-h3", "B-110", 5_400_000L, "left_edge", "sitting", true, false),
			new SeedEvent("evt-B-110-h4", "B-110", 9_000_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-101-h5", "B-101", 10_800_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-105-h6", "B-105", 10_800_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-103-h7", "B-103", 12_000_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-104-h8", "B-104", 12_600_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-108-h9", "B-108", 13_200_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-202-h10", "B-202", 13_800_000L, "center", "lying", true, false),
			new SeedEvent("evt-B-203-h11", "B-203", 14_400_000L, "center", "lying", true, true));

	// docs/backend-seed-data-spec.md §7 병상 상세 추가 이벤트 (14건)
	private static final List<SeedEvent> BED_DETAIL_SEED_EVENTS = List.of(
			new SeedEvent("evt-B-205-ph1", "B-205", 0L, "out_of_bed", "exit_attempt", false, false),
			new SeedEvent("evt-B-205-ph2", "B-205", 2_100_000L, "left_edge", "sitting", true, false),
			new SeedEvent("evt-B-205-ph3", "B-205", 5_400_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-205-ph4", "B-205", 10_800_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-110-ph1", "B-110", 300_000L, "right_edge", "exit_attempt", false, false),
			new SeedEvent("evt-B-110-ph2", "B-110", 3_000_000L, "left_edge", "sitting", true, false),
			new SeedEvent("evt-B-110-ph3", "B-110", 7_200_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-110-ph4", "B-110", 12_600_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-101-ph1", "B-101", 720_000L, "center", "sitting", true, false),
			new SeedEvent("evt-B-101-ph2", "B-101", 6_000_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-101-ph3", "B-101", 12_000_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-105-ph1", "B-105", 1_080_000L, "center", "sitting", true, false),
			new SeedEvent("evt-B-105-ph2", "B-105", 6_600_000L, "center", "lying", true, true),
			new SeedEvent("evt-B-105-ph3", "B-105", 12_000_000L, "center", "lying", true, true));

	private static final List<SeedEvent> SEED_EVENTS = Stream.of(
			CURRENT_STATUS_SEED_EVENTS, EVENT_LOG_SEED_EVENTS, BED_DETAIL_SEED_EVENTS)
			.flatMap(List::stream)
			.toList();

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
