package com.schemafy.domain.erd.relationship.domain.exception;

public class RelationshipTargetTableNotExistException extends RuntimeException {

  public RelationshipTargetTableNotExistException(String message) {
    super(message);
  }

}
