package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ConstraintColumnCountInvalidException extends InvalidValueException {

  public ConstraintColumnCountInvalidException(String message) {
    super(message);
  }

}
