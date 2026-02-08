package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipEmptyException extends DomainException {

  public RelationshipEmptyException(String message) {
    super(message);
  }

}
