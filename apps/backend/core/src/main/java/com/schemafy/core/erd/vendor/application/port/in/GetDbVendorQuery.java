package com.schemafy.core.erd.vendor.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;

public record GetDbVendorQuery(String displayName) {

  public GetDbVendorQuery {
    if (displayName == null || displayName.isBlank()) {
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "displayName must not be blank");
    }
  }

}
