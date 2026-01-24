package com.schemafy.domain.erd.relationship.domain.exception;

public class RelationshipCyclicReferenceException extends RuntimeException {

  public RelationshipCyclicReferenceException(String message) {
    super(message);
  }

}
