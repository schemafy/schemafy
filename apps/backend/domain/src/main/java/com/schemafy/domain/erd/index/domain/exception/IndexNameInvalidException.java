package com.schemafy.domain.erd.index.domain.exception;

import com.schemafy.domain.common.exception.InvalidValueException;


public class IndexNameInvalidException extends InvalidValueException {

  public IndexNameInvalidException(String message) {
    super(message);
  }

}
