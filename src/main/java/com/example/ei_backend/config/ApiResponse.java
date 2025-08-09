package com.example.ei_backend.config;


import com.example.ei_backend.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private int status;        // HTTP 상태 코드
    private boolean success;   // 성공 여부
    private String code;       // 비즈니스 코드 (e.g. S000, E401)
    private String message;    // 사용자/개발자 메시지
    private T data;            // 응답 데이터

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .success(true)
                .message("OK")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fail(ErrorCode ec, String message) {
        return ApiResponse.<T>builder()
                .status(ec.getStatus().value())
                .success(false)
                .code(ec.getCode())
                .message(message != null ? message : ec.getMessage())
                .build();

    }
}
