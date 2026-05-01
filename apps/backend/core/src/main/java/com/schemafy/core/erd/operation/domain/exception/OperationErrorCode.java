package com.schemafy.core.erd.operation.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperationErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  INVALID_VALUE(HttpStatus.BAD_REQUEST),
  SUPERSEDED(HttpStatus.CONFLICT),
  ALREADY_UNDONE(HttpStatus.CONFLICT),
  REDO_NOT_ELIGIBLE(HttpStatus.CONFLICT),
  UNSUPPORTED(HttpStatus.CONFLICT);

  private final HttpStatus status;

}
