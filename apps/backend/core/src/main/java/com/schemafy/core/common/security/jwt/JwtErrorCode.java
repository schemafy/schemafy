package com.schemafy.core.common.security.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtErrorCode {
    EXPIRED_TOKEN("EXPIRED_TOKEN", "만료된 토큰입니다."),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_TOKEN_TYPE("INVALID_TOKEN_TYPE", "액세스 토큰이 아닙니다."),
    MALFORMED_TOKEN("INVALID_TOKEN", "위조되거나 손상된 토큰입니다."),
    TOKEN_VALIDATION_ERROR("INVALID_TOKEN", "토큰 검증 중 오류가 발생했습니다.");

    private final String code;
    private final String message;
}