package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class ColumnNameInvalidException extends InvalidValueException {

  public ColumnNameInvalidException(String message) {
    super(message);
  }

}
