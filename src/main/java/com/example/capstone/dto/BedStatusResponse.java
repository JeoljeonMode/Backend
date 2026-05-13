package com.example.capstone.dto;

public record BedStatusResponse(
		String bedId,
		EventResponse status
) {
}
