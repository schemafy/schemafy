package com.schemafy.domain.erd.domain.exception;

public class RelationshipNotExistException extends RuntimeException {

  public RelationshipNotExistException(String message) {
    super(message);
  }

}
