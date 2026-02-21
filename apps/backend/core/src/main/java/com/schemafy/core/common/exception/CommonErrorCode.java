package com.schemafy.core.common.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements DomainErrorCode {

  SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_PARAMETER(HttpStatus.BAD_REQUEST),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
  ALREADY_DELETED(HttpStatus.CONFLICT),
  API_VERSION_MISSING(HttpStatus.BAD_REQUEST),
  API_VERSION_INVALID(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
