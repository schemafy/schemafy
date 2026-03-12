package com.schemafy.api.common.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuthErrorCode implements DomainErrorCode {

  CODE_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY),
  USER_INFO_FAILED(HttpStatus.BAD_GATEWAY),
  STATE_MISMATCH(HttpStatus.BAD_REQUEST),
  EMAIL_NOT_AVAILABLE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
