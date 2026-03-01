package com.schemafy.core.project.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements DomainErrorCode {

  NOT_FOUND(HttpStatus.NOT_FOUND),
  ACCESS_DENIED(HttpStatus.FORBIDDEN),
  OWNER_ONLY(HttpStatus.FORBIDDEN),
  ADMIN_REQUIRED(HttpStatus.FORBIDDEN),
  WORKSPACE_MISMATCH(HttpStatus.BAD_REQUEST),
  SETTINGS_TOO_LARGE(HttpStatus.BAD_REQUEST),
  ALREADY_DELETED(HttpStatus.CONFLICT),
  CANNOT_ASSIGN_HIGHER_ROLE(HttpStatus.BAD_REQUEST),
  CANNOT_MODIFY_HIGHER_ROLE_MEMBER(HttpStatus.BAD_REQUEST),
  CANNOT_CHANGE_OWN_ROLE(HttpStatus.BAD_REQUEST),
  LAST_OWNER_CANNOT_BE_REMOVED(HttpStatus.BAD_REQUEST),
  WORKSPACE_MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN),
  MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND),
  MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT);

  private final HttpStatus status;

}
