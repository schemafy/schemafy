package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ConstraintDefinitionDuplicateException extends DomainException {

  public ConstraintDefinitionDuplicateException(String message) {
    super(message);
  }

}
