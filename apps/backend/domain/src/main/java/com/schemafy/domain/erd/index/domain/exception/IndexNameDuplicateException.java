package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class IndexNameDuplicateException extends DomainException {

  public IndexNameDuplicateException(String message) {
    super(message);
  }

}
