package com.schemafy.domain.erd.table.domain.exception;

public class TableNotExistException extends RuntimeException {

  public TableNotExistException(String message) {
    super(message);
  }

}
