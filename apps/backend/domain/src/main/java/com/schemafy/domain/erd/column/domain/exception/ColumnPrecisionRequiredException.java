package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ColumnPrecisionRequiredException extends InvalidValueException {

  public ColumnPrecisionRequiredException(String message) {
    super(message);
  }

}
