package com.schemafy.domain.erd.schema.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class SchemaNameDuplicateException extends DomainException {

  public SchemaNameDuplicateException(String message) {
    super(message);
  }

}
