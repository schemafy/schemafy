package com.schemafy.domain.erd.relationship.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class RelationshipNameInvalidException extends InvalidValueException {

  public RelationshipNameInvalidException(String message) {
    super(message);
  }

}
