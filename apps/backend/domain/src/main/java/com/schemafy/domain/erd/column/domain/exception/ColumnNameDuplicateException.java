package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class ColumnNameDuplicateException extends DomainException {

  public ColumnNameDuplicateException(String message) {
    super(message);
  }

}
