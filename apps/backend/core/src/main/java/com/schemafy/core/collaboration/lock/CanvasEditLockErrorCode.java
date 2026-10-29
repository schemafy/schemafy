package com.schemafy.core.collaboration.lock;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CanvasEditLockErrorCode implements DomainErrorCode {

  REQUIRED(HttpStatus.CONFLICT),
  NOT_OWNER(HttpStatus.CONFLICT);

  private final HttpStatus status;

}
