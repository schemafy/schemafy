package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class IndexColumnNotExistException extends DomainException {

  public IndexColumnNotExistException(String message) {
    super(message);
  }

}
