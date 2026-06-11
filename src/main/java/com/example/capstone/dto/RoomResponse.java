package com.example.capstone.dto;

import java.util.List;

public record RoomResponse(
        String roomId,
        String label,
        String cameraId,
        String gender,
        int capacity,
        List<String> bedIds
) {
}
