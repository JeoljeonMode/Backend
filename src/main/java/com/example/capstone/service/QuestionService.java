package com.example.capstone.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.capstone.dto.EventResponse;

@Service
public class QuestionService {

	private final MonitoringService monitoringService;

	public QuestionService(MonitoringService monitoringService) {
		this.monitoringService = monitoringService;
	}

	public String answer(String question, String bedId) {
		EventResponse status;
		if (bedId != null && !bedId.isBlank()) {
			List<EventResponse> events = monitoringService.searchEvents(bedId, null, null, 1);
			status = events.isEmpty() ? monitoringService.currentStatus() : events.get(0);
		} else {
			status = monitoringService.currentStatus();
		}

		String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);

		if (normalized.contains("위험") || normalized.contains("risk")) {
			return "현재 %s 단계이고 위험 점수는 %d점입니다. %s"
					.formatted(status.riskLabel(), status.riskScore(), String.join(" ", status.riskFactors()));
		}
		if (normalized.contains("가드") || normalized.contains("rail")) {
			return status.guardrailUp() ? "침대 가드레일은 올라가 있습니다." : "침대 가드레일이 내려가 있어 확인이 필요합니다.";
		}
		if (normalized.contains("보호") || normalized.contains("caregiver")) {
			return status.caregiverPresent() ? "보호 인력이 감지되었습니다." : "현재 보호 인력이 감지되지 않았습니다.";
		}
		if (normalized.contains("자세") || normalized.contains("posture")) {
			return "현재 자세 상태는 %s입니다.".formatted(status.posture());
		}
		return "현재 %s, %s, 점수 %d점입니다. %s"
				.formatted(status.bedId(), status.riskLabel(), status.riskScore(), status.summary());
	}
}
