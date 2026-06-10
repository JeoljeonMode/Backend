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
			factors.add(toPositionFactor(request.patientPosition()));
		}
		if (Boolean.FALSE.equals(request.guardrailUp())) {
			score += 3;
			factors.add("침대 가드레일 내려감");
		}
		if (isRiskyPosture(request.posture())) {
			score += 2;
			factors.add("자세 상태: " + toKoreanPosture(request.posture()));
		}
		if (Boolean.FALSE.equals(request.caregiverPresent())) {
			score += 1;
			factors.add("보호 인력 미감지");
		}
		if (factors.isEmpty()) {
			factors.add("위험 인자 없음");
		}

		RiskLevel level = RiskLevel.fromScore(score);
		String name = request.patientName() != null ? request.patientName() : "환자";
		String summary = buildSummary(name, level);
		return new RiskAssessment(score, level, factors, summary);
	}

	private boolean isNearBedEdge(String position) {
		String value = normalize(position);
		return value.contains("edge") || value.contains("out_of_bed") || value.contains("out of bed")
				|| value.contains("가장자리") || value.contains("끝")
				|| value.contains("좌측") || value.contains("우측");
	}

	private boolean isRiskyPosture(String posture) {
		String value = normalize(posture);
		return value.contains("sitting") || value.contains("exit") || value.contains("leaving")
				|| value.contains("sit") || value.contains("앉") || value.contains("이탈");
	}

	private String toPositionFactor(String position) {
		return switch (normalize(position)) {
			case "left_edge" -> "침대 좌측 끝 근접";
			case "right_edge" -> "침대 우측 끝 근접";
			case "out_of_bed" -> "침상 밖";
			default -> "침대 끝 근접";
		};
	}

	private String toKoreanPosture(String posture) {
		return switch (normalize(posture)) {
			case "sitting", "sit" -> "앉음";
			case "exit_attempt", "exit", "leaving" -> "이탈 시도";
			default -> posture == null ? "위험 자세" : posture;
		};
	}

	private String buildSummary(String patientName, RiskLevel level) {
		return switch (level) {
			case DANGER -> "%s 환자에서 낙상 위험이 감지되었습니다.\n즉시 현장 확인이 필요합니다.".formatted(patientName);
			case CAUTION -> "%s 환자에서 주의가 필요합니다.\n침대 상태를 확인해 주세요.".formatted(patientName);
			case NORMAL -> "%s 환자의 상태가 안정적입니다.".formatted(patientName);
		};
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	public record RiskAssessment(int score, RiskLevel level, List<String> factors, String summary) {
	}
}
