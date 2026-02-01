package com.schemafy.domain.erd.schema.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class SchemaNotExistException extends DomainException {

  public SchemaNotExistException(String message) {
    super(message);
  }

}
