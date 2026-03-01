package com.schemafy.core.common.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements DomainErrorCode {

  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED(HttpStatus.FORBIDDEN),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
  INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED),
  MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
  INVALID_ACCESS_TOKEN_TYPE(HttpStatus.BAD_REQUEST),
  MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED),
  TOKEN_VALIDATION_ERROR(HttpStatus.UNAUTHORIZED);

  private final HttpStatus status;

}
