package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipColumnNotExistException extends DomainException {

  public RelationshipColumnNotExistException(String message) {
    super(message);
  }

}
