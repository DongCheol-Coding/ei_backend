package com.example.ei_backend.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    //  성공 응답용 static 메서드
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("ApiResponse 응답")
                .data(data)
                .build();
    }

    //  실패 응답용 static 메서드 (선택)
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(null)
                .build();
    }

}
