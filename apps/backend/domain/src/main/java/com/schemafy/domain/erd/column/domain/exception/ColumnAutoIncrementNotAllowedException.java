package com.schemafy.domain.erd.column.domain.exception;

public class ColumnAutoIncrementNotAllowedException extends RuntimeException {

  public ColumnAutoIncrementNotAllowedException(String message) {
    super(message);
  }

}
