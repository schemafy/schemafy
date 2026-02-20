package com.schemafy.domain.erd.vendor.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.vendor.domain.exception.VendorErrorCode;

public record GetDbVendorQuery(String displayName) {

  public GetDbVendorQuery {
    if (displayName == null || displayName.isBlank()) {
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "displayName must not be blank");
    }
  }

}
