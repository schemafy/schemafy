package com.schemafy.domain.erd.table.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TableErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  NAME_DUPLICATE(HttpStatus.CONFLICT),
  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
