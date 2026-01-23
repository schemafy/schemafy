package com.schemafy.domain.erd.domain.exception;

public class ColumnAutoIncrementNotAllowedException extends RuntimeException {

  public ColumnAutoIncrementNotAllowedException(String message) {
    super(message);
  }

}
