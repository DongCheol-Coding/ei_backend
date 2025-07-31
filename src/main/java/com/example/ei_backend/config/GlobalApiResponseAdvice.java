package com.example.ei_backend.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class GlobalApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 특정 클래스는 제외하고 싶은 경우 조건 작성 가능
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 이미 ApiResponse로 감싼 응답은 그대로 반환
        if (body instanceof ApiResponse) {
            return body;
        }

        // ResponseEntity로 직접 상태코드 설정한 경우도 그대로 반환
        if (body instanceof org.springframework.http.ResponseEntity) {
            return body;
        }

        return new ApiResponse<>(HttpStatus.OK.value(), "응답 성공", body);
    }
}
