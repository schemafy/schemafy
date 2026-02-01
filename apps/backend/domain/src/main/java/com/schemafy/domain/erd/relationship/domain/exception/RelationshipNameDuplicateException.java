package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipNameDuplicateException extends DomainException {

  public RelationshipNameDuplicateException(String message) {
    super(message);
  }

}
