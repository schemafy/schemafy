package com.schemafy.core.erd.ddl.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DdlErrorCode implements DomainErrorCode {

  INVALID_VALUE(HttpStatus.BAD_REQUEST),
  UNSUPPORTED_VENDOR(HttpStatus.BAD_REQUEST),
  TABLE_NOT_FOUND(HttpStatus.NOT_FOUND),
  COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND);

  private final HttpStatus status;

}
