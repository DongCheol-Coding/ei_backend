package com.example.ei_backend.exception;

import com.example.ei_backend.config.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 403 처리
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        om.writeValue(res.getWriter(), ApiResponse.fail(ErrorCode.FORBIDDEN, "접근 권한이 없습니다."));
    }
}