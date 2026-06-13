package com.example.capstone.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.dto.AlertRequest;
import com.example.capstone.dto.AlertResponse;
import com.example.capstone.repository.BedConfigStore;

@Service
public class AlertService {

	private static final Logger log = LoggerFactory.getLogger(AlertService.class);
	private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

	private final AtomicReference<AlertResponse> latest = new AtomicReference<>();
	private final Map<String, AlertResponse> latestByDeviceId = new ConcurrentHashMap<>();
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final BedConfigStore bedConfigStore;
	private final MonitoringService monitoringService;
	private final Map<String, String> deviceBedMap;
	private final String defaultBedId;

	public AlertService(BedConfigStore bedConfigStore, MonitoringService monitoringService, Environment environment,
			@Value("${app.jetson.default-bed-id:B-206}") String defaultBedId) {
		this.bedConfigStore = bedConfigStore;
		this.monitoringService = monitoringService;
		this.defaultBedId = defaultBedId;
		this.deviceBedMap = Binder.get(environment)
				.bind("app.jetson.device-bed-map", Bindable.mapOf(String.class, String.class))
				.orElse(Map.of());
	}

	public AlertResponse accept(AlertRequest request) {
		log.info("[VLM 알림 수신 시작] deviceId={} timestamp={} statusText={} snapshot={}",
				valueOrDefault(request.deviceId(), "jetson_orin_nano_bed_01"),
				request.timestamp(),
				preview(request.statusText()),
				snapshotSummary(request.snapshot()));
		AlertResponse response = AlertResponse.from(new AlertRequest(
				valueOrDefault(request.deviceId(), "jetson_orin_nano_bed_01"),
				request.timestamp() == null ? Instant.now().getEpochSecond() : request.timestamp(),
				valueOrDefault(request.statusText(), ""),
				request.snapshot()
		));
		latest.set(response);
		latestByDeviceId.put(response.deviceId(), response);
		reflectToMonitoringStatus(response);
		log.info("[VLM 알림 수신 완료] deviceId={} timestamp={} statusText={} snapshot={} sseSubscribers={}",
				response.deviceId(), response.timestamp(), preview(response.statusText()),
				snapshotSummary(response.snapshot()), emitters.size());
		broadcast(response);
		return response;
	}

	public AlertResponse latest(String deviceId) {
		log.info("[VLM 최신 알림 조회] deviceId={}", valueOrDefault(deviceId, "전체 최신"));
		AlertResponse response = deviceId == null || deviceId.isBlank()
				? latest.get()
				: latestByDeviceId.get(deviceId);
		if (response == null) {
			log.warn("[VLM 최신 알림 조회 실패] deviceId={} reason=아직 수신된 알림 없음",
					valueOrDefault(deviceId, "전체 최신"));
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No alert has been received yet");
		}
		log.info("[VLM 최신 알림 조회 성공] deviceId={} timestamp={} statusText={}",
				response.deviceId(), response.timestamp(), preview(response.statusText()));
		return response;
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		emitters.add(emitter);
		log.info("[VLM SSE 연결] 현재구독자수={}", emitters.size());
		emitter.onCompletion(() -> {
			emitters.remove(emitter);
			log.info("[VLM SSE 종료] reason=completion 현재구독자수={}", emitters.size());
		});
		emitter.onTimeout(() -> {
			emitters.remove(emitter);
			log.warn("[VLM SSE 종료] reason=timeout 현재구독자수={}", emitters.size());
		});
		emitter.onError(error -> {
			emitters.remove(emitter);
			log.warn("[VLM SSE 오류] reason={} 현재구독자수={}",
					error == null ? "unknown" : error.getMessage(), emitters.size());
		});

		AlertResponse response = latest.get();
		if (response != null) {
			send(emitter, response);
		}
		return emitter;
	}

	private void broadcast(AlertResponse response) {
		for (SseEmitter emitter : emitters) {
			send(emitter, response);
		}
	}

	private void send(SseEmitter emitter, AlertResponse response) {
		try {
			emitter.send(SseEmitter.event().name("alert").data(response));
		}
		catch (Exception e) {
			emitters.remove(emitter);
			log.warn("[VLM SSE 전송 실패] deviceId={} timestamp={} exception={} message={}",
					response.deviceId(), response.timestamp(), e.getClass().getName(), e.getMessage());
		}
	}

	private String valueOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private void reflectToMonitoringStatus(AlertResponse response) {
		String bedId = resolveBedId(response.deviceId());
		log.info("[VLM 알림 대시보드 반영 시작] deviceId={} mappedBedId={}", response.deviceId(), bedId);
		bedConfigStore.findByBedId(bedId)
				.ifPresentOrElse(
						bedConfig -> {
							monitoringService.acceptAlertEvent(toRequest(response), bedConfig);
							log.info("[VLM 알림 대시보드 반영 완료] deviceId={} bedId={} patientNo={} patientName={}",
									response.deviceId(), bedConfig.getBedId(), bedConfig.getPatientNo(),
									bedConfig.getPatientName());
						},
						() -> log.warn("[VLM 알림 반영 생략] deviceId={} bedId={} 병상 설정 없음",
								response.deviceId(), bedId)
				);
	}

	private String resolveBedId(String deviceId) {
		if (deviceId != null && deviceBedMap.containsKey(deviceId)) {
			return deviceBedMap.get(deviceId);
		}
		return defaultBedId;
	}

	private AlertRequest toRequest(AlertResponse response) {
		return new AlertRequest(response.deviceId(), response.timestamp(), response.statusText(), response.snapshot());
	}

	private String preview(String value) {
		if (value == null || value.isBlank()) {
			return "(비어 있음)";
		}
		String normalized = value.replaceAll("\\s+", " ").trim();
		return normalized.length() > 120 ? normalized.substring(0, 120) + "...(생략)" : normalized;
	}

	private String snapshotSummary(String snapshot) {
		if (snapshot == null || snapshot.isBlank()) {
			return "없음";
		}
		return "있음(length=" + snapshot.length() + ")";
	}
}
