package com.schemafy.domain.erd.table.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class TableNameDuplicateException extends DomainException {

  public TableNameDuplicateException(String message) {
    super(message);
  }

}
