package com.example.ei_backend.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {

    public static ResponseCookie makeRefreshCookie(
            String name, String token, String domainOrNull, String path, long days, boolean isHttps) {

        var b = ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(isHttps)                       // HTTPS면 true
                .sameSite(isHttps ? "None" : "Lax")    // HTTP는 Lax
                .path(path)
                .maxAge(Duration.ofDays(days));

        if (domainOrNull != null && !domainOrNull.isBlank()) {
            b.domain(domainOrNull);                    // 필요할 때만 설정
        }
        return b.build();
    }

    public static ResponseCookie deleteCookie(
            String name, String domainOrNull, String path, boolean isHttps) {

        var b = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isHttps)
                .sameSite(isHttps ? "None" : "Lax")
                .path(path)
                .maxAge(0);

        if (domainOrNull != null && !domainOrNull.isBlank()) {
            b.domain(domainOrNull);
        }
        return b.build();
    }
}

