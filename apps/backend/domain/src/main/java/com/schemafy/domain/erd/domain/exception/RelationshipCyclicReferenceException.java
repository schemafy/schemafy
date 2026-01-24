package com.schemafy.domain.erd.domain.exception;

public class RelationshipCyclicReferenceException extends RuntimeException {

  public RelationshipCyclicReferenceException(String message) {
    super(message);
  }

}
