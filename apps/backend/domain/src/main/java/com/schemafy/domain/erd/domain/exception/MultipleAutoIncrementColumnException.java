package com.schemafy.domain.erd.domain.exception;

public class MultipleAutoIncrementColumnException extends RuntimeException {

  public MultipleAutoIncrementColumnException(String message) {
    super(message);
  }

}
