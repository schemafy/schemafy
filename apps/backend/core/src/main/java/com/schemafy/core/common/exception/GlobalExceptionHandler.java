package com.schemafy.core.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.domain.common.exception.DomainErrorCode;
import com.schemafy.domain.common.exception.DomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<BaseResponse<Object>> handleDomainException(
      DomainException e) {
    DomainErrorCode errorCode = e.getErrorCode();
    log.warn("[DomainException] code={}, message={}", errorCode.code(),
        e.getMessage(), e);
    return ResponseEntity.status(errorCode.status())
        .body(BaseResponse.error(errorCode.code(), e.getMessage()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<BaseResponse<Object>> handleAuthorizationDeniedException(
      AuthorizationDeniedException e) {
    DomainErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;
    log.warn("AuthorizationDeniedException: {}", e.getMessage());
    return ResponseEntity.status(errorCode.status())
        .body(BaseResponse.error(errorCode.code(), e.getMessage()));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<BaseResponse<Object>> handleValidationException(
      WebExchangeBindException e) {
    DomainErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
    log.warn("[Validation] {}", e.getMessage());
    return ResponseEntity.status(errorCode.status())
        .body(BaseResponse.error(errorCode.code(), e.getReason()));
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<BaseResponse<Object>> handleServerWebInputException(
      ServerWebInputException e) {
    DomainErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
    String reason = e.getReason();
    String message = (reason != null && !reason.isBlank())
        ? reason
        : "Invalid request input";
    log.warn("[ServerWebInputException] {}", e.getMessage());
    return ResponseEntity.status(errorCode.status())
        .body(BaseResponse.error(errorCode.code(), message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Object>> handleException(Exception e) {
    DomainErrorCode errorCode = CommonErrorCode.SYSTEM_ERROR;
    log.error("[Unhandled] {}", e.getMessage(), e);
    return ResponseEntity.status(errorCode.status())
        .body(BaseResponse.error(errorCode.code(), "시스템 오류가 발생했습니다."));
  }

}
