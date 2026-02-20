package com.schemafy.core.common.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValidationErrorCode implements DomainErrorCode {

  FAILED(HttpStatus.BAD_REQUEST),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
  TIMEOUT(HttpStatus.REQUEST_TIMEOUT),
  ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

}
