package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ConstraintNameInvalidException extends InvalidValueException {

  public ConstraintNameInvalidException(String message) {
    super(message);
  }

}
