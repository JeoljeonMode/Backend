package com.example.capstone.dto;

public record BedCreateResponse(
        String bedId,
        String roomId,
        String patientName,
        String patientNo
) {
}
