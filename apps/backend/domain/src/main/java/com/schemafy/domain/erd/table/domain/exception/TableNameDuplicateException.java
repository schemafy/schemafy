package com.schemafy.domain.erd.table.domain.exception;

public class TableNameDuplicateException extends RuntimeException {

  public TableNameDuplicateException(String message) {
    super(message);
  }

}
