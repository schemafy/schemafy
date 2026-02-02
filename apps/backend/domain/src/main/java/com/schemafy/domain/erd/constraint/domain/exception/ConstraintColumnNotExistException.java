package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ConstraintColumnNotExistException extends DomainException {

  public ConstraintColumnNotExistException(String message) {
    super(message);
  }

}
