package com.example.capstone.dto;

import java.time.Instant;
import java.util.List;

import com.example.capstone.domain.MonitoringEvent;
import com.example.capstone.domain.RiskLevel;

public record EventResponse(
		String id,
		Instant occurredAt,
		String cameraId,
		String bedId,
		String patientName,
		String patientNo,
		String patientPosition,
		String posture,
		boolean guardrailUp,
		boolean caregiverPresent,
		int riskScore,
		RiskLevel riskLevel,
		String riskLabel,
		List<String> riskFactors,
		String summary,
		String frameUrl,
		Box roi,
		Box patientBox,
		boolean acknowledged,
		Instant acknowledgedAt
) {
	public static EventResponse from(MonitoringEvent event) {
		return new EventResponse(
				event.getId(),
				event.getOccurredAt(),
				event.getCameraId(),
				event.getBedId(),
				event.getPatientName(),
				event.getPatientNo(),
				event.getPatientPosition(),
				event.getPosture(),
				event.isGuardrailUp(),
				event.isCaregiverPresent(),
				event.getRiskScore(),
				event.getRiskLevel(),
				event.getRiskLevel().getLabel(),
				event.getRiskFactors(),
				event.getSummary(),
				event.getFrameUrl(),
				Box.from(event.getRoi()),
				Box.from(event.getPatientBox()),
				event.isAcknowledged(),
				event.getAcknowledgedAt()
		);
	}

	public record Box(double x, double y, double width, double height) {
		static Box from(MonitoringEvent.DetectionBox box) {
			if (box == null) {
				return null;
			}
			return new Box(box.x(), box.y(), box.width(), box.height());
		}
	}
}
