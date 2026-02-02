package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ColumnNotExistException extends DomainException {

  public ColumnNotExistException(String message) {
    super(message);
  }

}
