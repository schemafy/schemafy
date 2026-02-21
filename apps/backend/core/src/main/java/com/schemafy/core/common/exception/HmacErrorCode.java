package com.schemafy.core.common.exception;

import org.springframework.http.HttpStatus;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HmacErrorCode implements DomainErrorCode {

  SIGNATURE_MISSING(HttpStatus.UNAUTHORIZED),
  SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED),
  TIMESTAMP_EXPIRED(HttpStatus.UNAUTHORIZED),
  NONCE_DUPLICATE(HttpStatus.UNAUTHORIZED);

  private final HttpStatus status;

}
