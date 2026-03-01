package com.schemafy.core.project.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShareLinkErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  EXPIRED(HttpStatus.GONE),
  REVOKED(HttpStatus.FORBIDDEN),
  INVALID(HttpStatus.UNAUTHORIZED),
  INVALID_PROJECT_ID(HttpStatus.BAD_REQUEST),
  INVALID_TOKEN_HASH(HttpStatus.BAD_REQUEST),
  INVALID_ROLE(HttpStatus.BAD_REQUEST),
  INVALID_EXPIRATION(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
