package com.schemafy.domain.erd.index.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IndexErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND),
  NAME_DUPLICATE(HttpStatus.CONFLICT),
  NAME_INVALID(HttpStatus.BAD_REQUEST),
  COLUMN_DUPLICATE(HttpStatus.BAD_REQUEST),
  COLUMN_SORT_DIRECTION_INVALID(HttpStatus.BAD_REQUEST),
  POSITION_INVALID(HttpStatus.BAD_REQUEST),
  DEFINITION_DUPLICATE(HttpStatus.CONFLICT),
  TYPE_INVALID(HttpStatus.BAD_REQUEST),
  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
