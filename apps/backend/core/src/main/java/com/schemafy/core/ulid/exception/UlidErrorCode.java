package com.schemafy.core.ulid.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UlidErrorCode implements DomainErrorCode {

  INVALID_VALUE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
