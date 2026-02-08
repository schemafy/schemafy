package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class MultiplePrimaryKeyConstraintException extends DomainException {

  public MultiplePrimaryKeyConstraintException(String message) {
    super(message);
  }

}
