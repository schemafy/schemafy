package com.schemafy.domain.erd.column.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class MultipleAutoIncrementColumnException extends DomainException {

  public MultipleAutoIncrementColumnException(String message) {
    super(message);
  }

}
