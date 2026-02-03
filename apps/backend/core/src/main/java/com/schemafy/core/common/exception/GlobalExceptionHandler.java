package com.schemafy.core.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<BaseResponse<Object>> handleBusinessException(
      BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    log.warn("[BusinessException] BusinessException occurred: {}",
        errorCode.getMessage(), e);
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<BaseResponse<Object>> handleAuthorizationDeniedException(
      AuthorizationDeniedException e) {
    ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
    log.warn("AuthorizationDeniedException occurred: {}", e.getMessage());
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<BaseResponse<Object>> handleValidationException(
      WebExchangeBindException e) {
    ErrorCode errorCode = ErrorCode.COMMON_INVALID_PARAMETER;
    log.warn("[WebExchangeBindException] Validation failed: {}",
        e.getMessage());
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(), e.getReason()));
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<BaseResponse<Object>> handleDomainException(
      DomainException e) {
    ErrorCode errorCode = mapDomainExceptionToErrorCode(e);
    log.warn("[DomainException] Domain exception occurred: {}",
        e.getMessage(), e);
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

  private ErrorCode mapDomainExceptionToErrorCode(DomainException e) {
    if (e instanceof SchemaNotExistException) {
      return ErrorCode.ERD_SCHEMA_NOT_FOUND;
    }
    if (e instanceof SchemaNameDuplicateException) {
      return ErrorCode.ERD_SCHEMA_NAME_DUPLICATE;
    }
    if (e instanceof TableNotExistException) {
      return ErrorCode.ERD_TABLE_NOT_FOUND;
    }
    if (e instanceof TableNameDuplicateException) {
      return ErrorCode.ERD_TABLE_NAME_DUPLICATE;
    }
    return ErrorCode.COMMON_SYSTEM_ERROR;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Object>> handleException(Exception e) {
    ErrorCode errorCode = ErrorCode.COMMON_SYSTEM_ERROR;
    log.error("[Exception] Unhandled exception occurred: {}",
        e.getMessage(), e);
    return ResponseEntity.status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

}
