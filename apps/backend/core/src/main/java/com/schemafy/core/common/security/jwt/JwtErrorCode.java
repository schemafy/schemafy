package com.schemafy.core.common.security.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JwtErrorCode {
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "J001", "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "J002", "유효하지 않은 토큰입니다."),
    INVALID_ACCESS_TOKEN_TYPE(HttpStatus.BAD_REQUEST, "J003", "액세스 토큰이 아닙니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "J004", "위조되거나 손상된 토큰입니다."),
    TOKEN_VALIDATION_ERROR(HttpStatus.UNAUTHORIZED, "J005", "토큰 검증 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}