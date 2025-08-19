package com.example.ei_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse; // Swagger의 ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test", description = "쿠키 동작 확인용 테스트 API")
@RestController
public class CookieTestController {

    @Operation(
            summary = "쿠키 테스트",
            description = "RT_TEST 쿠키를 내려 쿠키 동작을 확인합니다. 바디는 없고, Set-Cookie 헤더만 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "쿠키가 성공적으로 내려감",
                    headers = {
                            @Header(
                                    name = "Set-Cookie",
                                    description = "예: RT_TEST=ok; Path=/; HttpOnly; SameSite=Lax"
                            )
                    },
                    content = @Content // 바디 없음
            )
    })
    @GetMapping("/test/cookie")
    public void testCookie(HttpServletResponse res) {
        var c = org.springframework.http.ResponseCookie.from("RT_TEST","ok")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                // .secure(true)                // prod HTTPS면 주로 켭니다
                // .domain("dongcheolcoding.life") // 서브도메인 공유 시 지정
                .build();
        res.addHeader("Set-Cookie", c.toString());
    }
}
