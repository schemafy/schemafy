package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ColumnAutoIncrementNotAllowedException extends InvalidValueException {

  public ColumnAutoIncrementNotAllowedException(String message) {
    super(message);
  }

}
