package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;


public class ColumnCharsetNotAllowedException extends InvalidValueException {

  public ColumnCharsetNotAllowedException(String message) {
    super(message);
  }

}
