package com.example.ei_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT("E400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("E401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("E403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("E404", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    SERVER_ERROR("E500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_ROLE("E400", HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 역할입니다."),
    CONFLICT("E409", HttpStatus.CONFLICT, "리소스 충돌"),
    // 회원/인증
    EMAIL_ALREADY_EXISTS("U409", HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    VERIFICATION_NOT_FOUND("U404", HttpStatus.NOT_FOUND, "인증 요청이 없습니다."),
    VERIFICATION_JSON_ERROR("U500", HttpStatus.INTERNAL_SERVER_ERROR, "요청 직렬화 실패"),
    INVALID_VERIFY_CODE("U400", HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    USER_NOT_FOUND("U404", HttpStatus.NOT_FOUND, "존재하지 않는 사용자"),
    INVALID_CREDENTIALS("U401", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 잘못되었습니다."),
    PROFILE_IMAGE_NOT_FOUND("U404", HttpStatus.NOT_FOUND, "프로필 이미지가 없습니다."),

    // 도메인별 예시
    COURSE_NOT_FOUND("C404", HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    PAYMENT_FAILED("P400", HttpStatus.BAD_REQUEST, "결제 처리에 실패했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}