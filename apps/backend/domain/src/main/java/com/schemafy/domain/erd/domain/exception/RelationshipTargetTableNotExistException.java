package com.schemafy.domain.erd.domain.exception;

public class RelationshipTargetTableNotExistException extends RuntimeException {

  public RelationshipTargetTableNotExistException(String message) {
    super(message);
  }

}
