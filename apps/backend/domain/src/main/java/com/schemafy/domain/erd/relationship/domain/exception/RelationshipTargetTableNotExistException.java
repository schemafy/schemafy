package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class RelationshipTargetTableNotExistException extends DomainException {

  public RelationshipTargetTableNotExistException(String message) {
    super(message);
  }

}
