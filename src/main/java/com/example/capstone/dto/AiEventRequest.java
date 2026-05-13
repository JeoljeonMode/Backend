package com.example.capstone.dto;

import java.time.Instant;

public record AiEventRequest(
		Instant occurredAt,
		String cameraId,
		String bedId,
		String patientName,
		String patientPosition,
		String posture,
		Boolean guardrailUp,
		Boolean caregiverPresent,
		String frameUrl,
		Box roi,
		Box patientBox
) {
	public record Box(double x, double y, double width, double height) {
	}
}
