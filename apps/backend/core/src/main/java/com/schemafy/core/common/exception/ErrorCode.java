package com.schemafy.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    COMMON_SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "시스템 오류가 발생했습니다."),
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C002", "유효하지 않은 파라미터입니다."),

    // Authentication & Authorization
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다. 유효한 토큰을 제공해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 리프레시 토큰입니다."),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰 타입입니다."),
    MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰이 없습니다."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "회원을 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 존재하는 회원입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U003", "로그인에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
