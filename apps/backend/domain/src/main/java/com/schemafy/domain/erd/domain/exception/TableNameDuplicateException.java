package com.schemafy.domain.erd.domain.exception;

public class TableNameDuplicateException extends RuntimeException {

  public TableNameDuplicateException(String message) {
    super(message);
  }

}
