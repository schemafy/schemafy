package com.schemafy.domain.common.exception;

import lombok.Getter;

import java.util.function.Predicate;

@Getter
public class DomainException extends RuntimeException {

  private final DomainErrorCode errorCode;

  public DomainException(DomainErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public DomainException(DomainErrorCode errorCode) {
    super(errorCode.code());
    this.errorCode = errorCode;
  }

  public static Predicate<Throwable> hasErrorCode(DomainErrorCode code) {
    return e -> e instanceof DomainException de
        && de.getErrorCode() == code;
  }

}
