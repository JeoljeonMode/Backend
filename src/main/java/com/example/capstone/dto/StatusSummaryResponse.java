package com.example.capstone.dto;

public record StatusSummaryResponse(
		long totalEvents,
		long dangerEvents,
		long cautionEvents,
		long normalEvents,
		int latestRiskScore,
		String latestRiskLabel,
		String latestSummary
) {
}
