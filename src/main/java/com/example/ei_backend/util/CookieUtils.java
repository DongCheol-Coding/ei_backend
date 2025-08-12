package com.example.ei_backend.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {

    public static ResponseCookie makeRefreshCookie(String name, String token, String domain, String path, long days, boolean isHttps) {
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(isHttps) // HTTPS면 true, HTTP면 false
                .sameSite(isHttps ? "None" : "Lax") // HTTP라면 Lax로 설정
                .domain(domain)
                .path(path)
                .maxAge(Duration.ofDays(days))
                .build();
    }

    public static ResponseCookie deleteCookie(String name, String domain, String path, boolean isHttps) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isHttps)
                .sameSite(isHttps ? "None" : "Lax")
                .domain(domain)
                .path(path)
                .maxAge(0)
                .build();
    }
}
