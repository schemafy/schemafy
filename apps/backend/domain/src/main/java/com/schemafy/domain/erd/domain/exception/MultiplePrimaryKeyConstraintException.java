package com.schemafy.domain.erd.domain.exception;

public class MultiplePrimaryKeyConstraintException extends RuntimeException {

  public MultiplePrimaryKeyConstraintException(String message) {
    super(message);
  }

}
