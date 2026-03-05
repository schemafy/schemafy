package com.schemafy.domain.erd.memo.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
  ACCESS_DENIED(HttpStatus.FORBIDDEN),
  INVALID_PARAMETER(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
