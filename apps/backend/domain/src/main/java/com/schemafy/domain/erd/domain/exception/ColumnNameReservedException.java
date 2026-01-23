package com.schemafy.domain.erd.domain.exception;

public class ColumnNameReservedException extends RuntimeException {

  public ColumnNameReservedException(String message) {
    super(message);
  }

}
