package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ColumnLengthRequiredException extends InvalidValueException {

  public ColumnLengthRequiredException(String message) {
    super(message);
  }

}
