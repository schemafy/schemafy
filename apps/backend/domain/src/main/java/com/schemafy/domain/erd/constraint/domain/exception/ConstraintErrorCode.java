package com.schemafy.domain.erd.constraint.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConstraintErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND),
  NAME_DUPLICATE(HttpStatus.CONFLICT),
  NAME_INVALID(HttpStatus.BAD_REQUEST),
  COLUMN_DUPLICATE(HttpStatus.BAD_REQUEST),
  COLUMN_COUNT_INVALID(HttpStatus.BAD_REQUEST),
  POSITION_INVALID(HttpStatus.BAD_REQUEST),
  DEFINITION_DUPLICATE(HttpStatus.CONFLICT),
  EXPRESSION_REQUIRED(HttpStatus.BAD_REQUEST),
  MULTIPLE_PRIMARY_KEY(HttpStatus.BAD_REQUEST),
  UNIQUE_SAME_AS_PRIMARY_KEY(HttpStatus.BAD_REQUEST),
  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
