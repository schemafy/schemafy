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
  LOGIN_FAILED(HttpStatus.BAD_REQUEST),
  OAUTH_LINK_INCONSISTENT(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

}
