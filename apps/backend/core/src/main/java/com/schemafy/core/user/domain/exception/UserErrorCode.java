package com.schemafy.core.user.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  ALREADY_EXISTS(HttpStatus.CONFLICT),
  INVALID_PARAMETER(HttpStatus.BAD_REQUEST),
  LOGIN_FAILED(HttpStatus.BAD_REQUEST),
  EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
  ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN),
  VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST),
  VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST),
  VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),
  AUTH_MAIL_DISABLED(HttpStatus.CONFLICT),
  EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE),
  OAUTH_LINK_INCONSISTENT(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

}
