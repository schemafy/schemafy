package com.schemafy.core.project.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  ACCESS_DENIED(HttpStatus.FORBIDDEN),
  SETTINGS_INVALID(HttpStatus.BAD_REQUEST),
  SETTINGS_TOO_LARGE(HttpStatus.BAD_REQUEST),
  ALREADY_DELETED(HttpStatus.CONFLICT),
  ADMIN_REQUIRED(HttpStatus.FORBIDDEN),
  MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND),
  MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST),
  LAST_ADMIN_CANNOT_LEAVE(HttpStatus.BAD_REQUEST),
  LAST_ADMIN_CANNOT_CHANGE_ROLE(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

}
