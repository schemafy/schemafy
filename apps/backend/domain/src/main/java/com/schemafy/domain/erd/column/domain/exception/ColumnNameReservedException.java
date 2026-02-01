package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ColumnNameReservedException extends DomainException {

  public ColumnNameReservedException(String message) {
    super(message);
  }

}
