package com.example.capstone.dto;

public record AdminUserResponse(
        String username,
        String displayName,
        String role
) {
    public static AdminUserResponse from(AuthResponse response) {
        return new AdminUserResponse(response.username(), response.displayName(), response.role());
    }
}
