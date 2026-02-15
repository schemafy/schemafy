package com.schemafy.domain.common.exception;

public class InvalidValueException extends DomainException {

  public InvalidValueException(String message) {
    super(message);
  }

  public InvalidValueException(String message, Throwable cause) {
    super(message, cause);
  }

}
