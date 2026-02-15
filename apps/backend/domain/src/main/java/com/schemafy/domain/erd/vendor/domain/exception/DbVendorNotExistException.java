package com.schemafy.domain.erd.vendor.domain.exception;

import com.schemafy.domain.common.exception.DomainException;

public class DbVendorNotExistException extends DomainException {

  public DbVendorNotExistException(String message) {
    super(message);
  }

}
