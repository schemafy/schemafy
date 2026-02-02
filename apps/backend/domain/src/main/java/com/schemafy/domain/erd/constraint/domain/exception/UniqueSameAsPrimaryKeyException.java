package com.schemafy.domain.erd.constraint.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class UniqueSameAsPrimaryKeyException extends DomainException {

  public UniqueSameAsPrimaryKeyException(String message) {
    super(message);
  }

}
