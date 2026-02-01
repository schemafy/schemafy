package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class IndexNotExistException extends DomainException {

  public IndexNotExistException(String message) {
    super(message);
  }

}
