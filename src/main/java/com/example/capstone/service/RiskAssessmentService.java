package com.example.capstone.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.capstone.domain.RiskLevel;
import com.example.capstone.dto.AiEventRequest;

@Service
public class RiskAssessmentService {

	public RiskAssessment assess(AiEventRequest request) {
		int score = 0;
		List<String> factors = new ArrayList<>();

		if (isNearBedEdge(request.patientPosition())) {
			score += 3;
			factors.add("환자가 침대 가장자리 근처에 있습니다.");
		}
		if (Boolean.FALSE.equals(request.guardrailUp())) {
			score += 3;
			factors.add("침대 가드레일이 내려가 있습니다.");
		}
		if (isRiskyPosture(request.posture())) {
			score += 2;
			factors.add("자세가 " + toKoreanPosture(request.posture()) + " 상태입니다.");
		}
		if (Boolean.FALSE.equals(request.caregiverPresent())) {
			score += 2;
			factors.add("보호 인력이 감지되지 않았습니다.");
		}
		if (factors.isEmpty()) {
			factors.add("위험 인자가 감지되지 않았습니다.");
		}

		RiskLevel level = RiskLevel.fromScore(score);
		String summary = "%s 상태입니다. 위험 점수는 %d점입니다.".formatted(level.getLabel(), score);
		return new RiskAssessment(score, level, factors, summary);
	}

	private boolean isNearBedEdge(String position) {
		String value = normalize(position);
		return value.contains("edge") || value.contains("left_edge") || value.contains("right_edge")
				|| value.contains("bed_edge") || value.contains("가장자리") || value.contains("끝")
				|| value.contains("좌측") || value.contains("우측");
	}

	private boolean isRiskyPosture(String posture) {
		String value = normalize(posture);
		return value.contains("sitting") || value.contains("exit") || value.contains("leaving")
				|| value.contains("sit") || value.contains("앉") || value.contains("이탈");
	}

	private String toKoreanPosture(String posture) {
		return switch (normalize(posture)) {
			case "sitting", "sit" -> "앉음";
			case "exit_attempt", "exit", "leaving" -> "이탈 시도";
			default -> posture == null ? "위험 자세" : posture;
		};
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private String fallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	public record RiskAssessment(int score, RiskLevel level, List<String> factors, String summary) {
	}
}
