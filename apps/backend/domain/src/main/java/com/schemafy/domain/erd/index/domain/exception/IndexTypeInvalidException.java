package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;

public class IndexTypeInvalidException extends InvalidValueException {

  public IndexTypeInvalidException(String message) {
    super(message);
  }

}
