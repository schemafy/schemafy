package com.schemafy.core.erd.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND);

  private final HttpStatus status;

}
