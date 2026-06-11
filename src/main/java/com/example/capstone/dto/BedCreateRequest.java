package com.example.capstone.dto;

public record BedCreateRequest(
        String bedId,
        String roomId,
        String patientName,
        String patientNo
) {
}
