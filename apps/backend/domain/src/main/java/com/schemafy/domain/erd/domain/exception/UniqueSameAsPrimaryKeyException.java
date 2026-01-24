package com.schemafy.domain.erd.domain.exception;

public class UniqueSameAsPrimaryKeyException extends RuntimeException {

  public UniqueSameAsPrimaryKeyException(String message) {
    super(message);
  }

}
