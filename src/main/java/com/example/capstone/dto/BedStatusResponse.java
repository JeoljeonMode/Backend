package com.example.capstone.dto;

public record BedStatusResponse(
		String bedId,
		String roomId,
		String cameraId,
		EventResponse status
) {
}
