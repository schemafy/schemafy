package com.schemafy.domain.erd.domain.exception;

public class ColumnLengthRequiredException extends RuntimeException {

  public ColumnLengthRequiredException(String message) {
    super(message);
  }

}
