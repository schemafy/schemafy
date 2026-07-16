package com.schemafy.core.erd.vendor.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;

public record GetDbVendorQuery(String id) {

  public GetDbVendorQuery {
    if (id == null || id.isBlank()) {
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "id must not be blank");
    }
  }

}
