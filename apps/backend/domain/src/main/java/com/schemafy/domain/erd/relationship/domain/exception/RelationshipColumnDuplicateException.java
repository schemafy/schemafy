package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipColumnDuplicateException extends DomainException {

  public RelationshipColumnDuplicateException(String message) {
    super(message);
  }

}
