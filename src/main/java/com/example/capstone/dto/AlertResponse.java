package com.example.capstone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlertResponse(
		@JsonProperty("device_id")
		String deviceId,
		Long timestamp,
		@JsonProperty("status_text")
		String statusText,
		String snapshot
) {
	public static AlertResponse from(AlertRequest request) {
		return new AlertResponse(
				request.deviceId(),
				request.timestamp(),
				request.statusText(),
				request.snapshot()
		);
	}
}
