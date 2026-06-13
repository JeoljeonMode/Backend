package com.example.capstone.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.capstone.dto.AlertRequest;
import com.example.capstone.dto.AlertResponse;

@Service
public class AlertService {

	private static final Logger log = LoggerFactory.getLogger(AlertService.class);
	private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

	private final AtomicReference<AlertResponse> latest = new AtomicReference<>();
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	public AlertResponse accept(AlertRequest request) {
		AlertResponse response = AlertResponse.from(new AlertRequest(
				valueOrDefault(request.deviceId(), "jetson_orin_nano_bed_01"),
				request.timestamp() == null ? Instant.now().getEpochSecond() : request.timestamp(),
				valueOrDefault(request.statusText(), ""),
				request.snapshot()
		));
		latest.set(response);
		log.info("[VLM 알림 수신] deviceId={} timestamp={} statusTextLength={} snapshot={}",
				response.deviceId(), response.timestamp(), response.statusText().length(),
				response.snapshot() == null || response.snapshot().isBlank() ? "empty" : "present");
		broadcast(response);
		return response;
	}

	public AlertResponse latest() {
		AlertResponse response = latest.get();
		if (response == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No alert has been received yet");
		}
		return response;
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(error -> emitters.remove(emitter));

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
		catch (Exception ignored) {
			emitters.remove(emitter);
		}
	}

	private String valueOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
