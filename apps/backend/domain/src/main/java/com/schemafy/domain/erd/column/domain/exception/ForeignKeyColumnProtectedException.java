package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ForeignKeyColumnProtectedException extends DomainException {

  public ForeignKeyColumnProtectedException(String message) {
    super(message);
  }

}
