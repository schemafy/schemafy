package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ConstraintColumnDuplicateException extends DomainException {

  public ConstraintColumnDuplicateException(String message) {
    super(message);
  }

}
