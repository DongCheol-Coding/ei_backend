package com.example.ei_backend.domain.dto.auth;

import java.util.List;

public record LoginResult(
        Long userId,
        String email,
        List<String> roles,
        String accessToken,
        String refreshToken
) {}