package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class IndexDefinitionDuplicateException extends DomainException {

  public IndexDefinitionDuplicateException(String message) {
    super(message);
  }

}
