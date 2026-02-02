package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipNotExistException extends DomainException {

  public RelationshipNotExistException(String message) {
    super(message);
  }

}
