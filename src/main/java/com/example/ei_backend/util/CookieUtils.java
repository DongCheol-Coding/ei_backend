package com.example.ei_backend.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

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

    public static ResponseCookie makeCookieSeconds(
            String name, String token, String domainOrNull, String path, long maxAgeSeconds, boolean isHttps) {

        var b = ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(isHttps)
                .sameSite(isHttps ? "None" : "Lax")
                .path(path)
                .maxAge(maxAgeSeconds);

        if (domainOrNull != null && !domainOrNull.isBlank()) {
            b.domain(domainOrNull);
        }
        return b.build();
    }

    /** 요청에서 특정 이름의 쿠키를 Optional로 반환 (null-safe) */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null || name == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .findFirst();
    }

    /** 값이 바로 필요할 때 편의용 (없으면 null) */
    public static String getCookieValue(HttpServletRequest request, String name) {
        return getCookie(request, name).map(Cookie::getValue).orElse(null);
    }

}

