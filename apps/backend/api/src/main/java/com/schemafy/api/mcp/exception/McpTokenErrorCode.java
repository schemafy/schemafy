package com.schemafy.api.mcp.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.core.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum McpTokenErrorCode implements DomainErrorCode {

  INVALID(HttpStatus.UNAUTHORIZED),
  OWNER_MISMATCH(HttpStatus.FORBIDDEN),
  REVOCATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
  REGISTRY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

  private final HttpStatus status;

}
