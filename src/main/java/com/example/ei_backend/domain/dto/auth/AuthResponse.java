package com.example.ei_backend.domain.dto.auth;

public record AuthResponse(String accessToken, UserProfile user) {

}
