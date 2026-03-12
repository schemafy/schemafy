package com.schemafy.core.project.domain.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShareLinkErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  INVALID_PROJECT_ID(HttpStatus.BAD_REQUEST),
  INVALID_LINK(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
