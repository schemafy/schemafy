package com.schemafy.core.common.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.validation.exception.ValidationFailedException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<BaseResponse<Object>> handleValidationFailedException(
            ValidationFailedException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn(
                "[ValidationFailedException] ValidationFailedException occurred with {} errors",
                e.getValidationErrors().size());

        String detailedMessage = e.getValidationErrors().stream()
                .map(ValidationFailedException.ValidationError::toString)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), detailedMessage,
                        e.getValidationErrors()));
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleException(Exception e) {
        if (e instanceof StatusRuntimeException statusEx) {
            Status.Code code = statusEx.getStatus().getCode();
            if (code == Status.Code.UNAVAILABLE) {
                ErrorCode errorCode = ErrorCode.VALIDATION_SERVICE_UNAVAILABLE;
                log.warn("[Exception] gRPC UNAVAILABLE: {}",
                        statusEx.getMessage());
                return ResponseEntity.status(errorCode.getStatus()).body(
                        BaseResponse.error(errorCode.getCode(),
                                errorCode.getMessage()));
            }
            if (code == Status.Code.DEADLINE_EXCEEDED) {
                ErrorCode errorCode = ErrorCode.VALIDATION_TIMEOUT;
                log.warn("[Exception] gRPC DEADLINE_EXCEEDED: {}",
                        statusEx.getMessage());
                return ResponseEntity.status(errorCode.getStatus()).body(
                        BaseResponse.error(errorCode.getCode(),
                                errorCode.getMessage()));
            }
            ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
            log.error("[Exception] gRPC error: {}", statusEx.getMessage(),
                    statusEx);
            return ResponseEntity.status(errorCode.getStatus()).body(
                    BaseResponse.error(errorCode.getCode(),
                            errorCode.getMessage()));
        }

        ErrorCode errorCode = ErrorCode.COMMON_SYSTEM_ERROR;
        log.error("[Exception] Unhandled exception occurred: {}",
                e.getMessage(), e);
        return ResponseEntity.status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(),
                        errorCode.getMessage()));
    }

}
