package com.schemafy.domain.erd.relationship.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RelationshipErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND),
  TARGET_TABLE_NOT_FOUND(HttpStatus.NOT_FOUND),
  NAME_DUPLICATE(HttpStatus.CONFLICT),
  NAME_INVALID(HttpStatus.BAD_REQUEST),
  COLUMN_DUPLICATE(HttpStatus.BAD_REQUEST),
  POSITION_INVALID(HttpStatus.BAD_REQUEST),
  EMPTY(HttpStatus.BAD_REQUEST),
  CYCLIC_REFERENCE(HttpStatus.BAD_REQUEST),
  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
