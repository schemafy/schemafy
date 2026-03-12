package com.schemafy.core.erd.schema.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SchemaErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  NAME_DUPLICATE(HttpStatus.CONFLICT),
  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
