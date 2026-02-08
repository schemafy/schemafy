package com.schemafy.domain.erd.table.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class TableNotExistException extends DomainException {

  public TableNotExistException(String message) {
    super(message);
  }

}
