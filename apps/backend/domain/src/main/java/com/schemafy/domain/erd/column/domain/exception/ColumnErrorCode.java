package com.schemafy.domain.erd.column.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ColumnErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  NAME_DUPLICATE(HttpStatus.CONFLICT),
  NAME_RESERVED(HttpStatus.BAD_REQUEST),
  NAME_INVALID(HttpStatus.BAD_REQUEST),
  AUTO_INCREMENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST),
  CHARSET_NOT_ALLOWED(HttpStatus.BAD_REQUEST),
  DATA_TYPE_INVALID(HttpStatus.BAD_REQUEST),
  LENGTH_REQUIRED(HttpStatus.BAD_REQUEST),
  PRECISION_REQUIRED(HttpStatus.BAD_REQUEST),
  POSITION_INVALID(HttpStatus.BAD_REQUEST),
  FK_PROTECTED(HttpStatus.BAD_REQUEST),
  MULTIPLE_AUTO_INCREMENT(HttpStatus.BAD_REQUEST),
  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
