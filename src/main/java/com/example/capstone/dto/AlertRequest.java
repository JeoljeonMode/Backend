package com.example.capstone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlertRequest(
		@JsonProperty("device_id")
		String deviceId,
		Long timestamp,
		@JsonProperty("status_text")
		String statusText,
		String snapshot
) {
}
