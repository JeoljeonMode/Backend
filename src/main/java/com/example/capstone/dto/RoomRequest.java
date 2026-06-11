package com.example.capstone.dto;

public record RoomRequest(
        String roomId,
        String label,
        String cameraId,
        String gender,
        int capacity
) {
}
