package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ConstraintExpressionRequiredException extends InvalidValueException {

  public ConstraintExpressionRequiredException(String message) {
    super(message);
  }

}
