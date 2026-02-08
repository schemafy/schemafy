package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipCyclicReferenceException extends DomainException {

  public RelationshipCyclicReferenceException(String message) {
    super(message);
  }

}
