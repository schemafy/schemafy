package com.schemafy.core.user.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  ALREADY_EXISTS(HttpStatus.CONFLICT),
  LOGIN_FAILED(HttpStatus.UNAUTHORIZED);

  private final HttpStatus status;

}
