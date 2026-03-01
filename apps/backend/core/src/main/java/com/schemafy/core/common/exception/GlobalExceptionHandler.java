package com.schemafy.core.common.exception;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.schemafy.domain.common.exception.DomainErrorCode;
import com.schemafy.domain.common.exception.DomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final ObjectProvider<ProblemDetailFactory> problemDetailFactoryProvider;

  public GlobalExceptionHandler(
      ObjectProvider<ProblemDetailFactory> problemDetailFactoryProvider) {
    this.problemDetailFactoryProvider = problemDetailFactoryProvider;
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomainException(
      DomainException e, ServerWebExchange exchange) {
    DomainErrorCode errorCode = e.getErrorCode();
    log.warn("[DomainException] code={}, message={}", errorCode.code(),
        e.getMessage(), e);
    return buildResponse(exchange, errorCode, e.getMessage());
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAuthorizationDeniedException(
      AuthorizationDeniedException e, ServerWebExchange exchange) {
    DomainErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;
    log.warn("AuthorizationDeniedException: {}", e.getMessage());
    return buildResponse(exchange, errorCode, e.getMessage());
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      WebExchangeBindException e, ServerWebExchange exchange) {
    DomainErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
    log.warn("[Validation] {}", e.getMessage());
    return buildResponse(exchange, errorCode, e.getReason());
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ProblemDetail> handleServerWebInputException(
      ServerWebInputException e, ServerWebExchange exchange) {
    DomainErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
    log.warn("[ServerWebInputException] {}", e.getMessage());
    return buildResponse(exchange, errorCode, e.getReason());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleException(Exception e,
      ServerWebExchange exchange) {
    DomainErrorCode errorCode = CommonErrorCode.SYSTEM_ERROR;
    log.error("[Unhandled] {}", e.getMessage(), e);
    return buildResponse(exchange, errorCode, "시스템 오류가 발생했습니다.");
  }

  private ResponseEntity<ProblemDetail> buildResponse(
      ServerWebExchange exchange, DomainErrorCode errorCode,
      String message) {
    ProblemDetailFactory problemDetailFactory = problemDetailFactoryProvider
        .getIfAvailable(
            () -> new ProblemDetailFactory(new ProblemProperties()));
    ProblemDetail problemDetail = problemDetailFactory.create(exchange,
        errorCode, message);
    return ResponseEntity.status(errorCode.status())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetail);
  }

}
