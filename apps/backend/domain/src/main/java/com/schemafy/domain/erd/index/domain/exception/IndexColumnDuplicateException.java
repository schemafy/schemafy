package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class IndexColumnDuplicateException extends DomainException {

  public IndexColumnDuplicateException(String message) {
    super(message);
  }

}
