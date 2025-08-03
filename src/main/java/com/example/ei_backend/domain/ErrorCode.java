package com.example.ei_backend.domain;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_ROLE(HttpStatus.BAD_REQUEST, "잘못된 사용자 역할입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "검색 조건을 입력해주세요."),
    // 필요에 따라 계속 추가 가능

    ;

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
