package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ConstraintNameDuplicateException extends DomainException {

  public ConstraintNameDuplicateException(String message) {
    super(message);
  }

}
