package com.schemafy.core.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.schemafy.core.common.type.BaseResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Object>> handleBusinessException(
            BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException occurred: {}", errorCode.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(),
                        errorCode.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<BaseResponse<Object>> handleValidationException(
            WebExchangeBindException e) {
        ErrorCode errorCode = ErrorCode.COMMON_INVALID_PARAMETER;
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.COMMON_SYSTEM_ERROR;
        log.error("Unhandled exception occurred: {}", e.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(),
                        errorCode.getMessage()));
    }
}
