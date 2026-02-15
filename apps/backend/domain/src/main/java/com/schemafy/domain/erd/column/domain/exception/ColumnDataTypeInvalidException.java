package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ColumnDataTypeInvalidException extends InvalidValueException {

  public ColumnDataTypeInvalidException(String message) {
    super(message);
  }

}
