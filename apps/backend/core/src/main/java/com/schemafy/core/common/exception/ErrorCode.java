package com.schemafy.core.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    COMMON_SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001",
            "시스템 오류가 발생했습니다."),
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C002",
            "유효하지 않은 파라미터입니다."),
    COMMON_API_VERSION_MISSING(HttpStatus.BAD_REQUEST, "C003",
            "API 버전이 누락되었습니다."),
    COMMON_API_VERSION_INVALID(HttpStatus.BAD_REQUEST, "C004",
            "유효하지 않은 API 버전 형식입니다. (예: v1.0, v2.1)"),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "회원을 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 존재하는 회원입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U003", "로그인에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
