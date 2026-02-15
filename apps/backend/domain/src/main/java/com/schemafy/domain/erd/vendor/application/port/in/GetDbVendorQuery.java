package com.schemafy.domain.erd.vendor.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetDbVendorQuery(String displayName) {

  public GetDbVendorQuery {
    if (displayName == null || displayName.isBlank()) {
      throw new InvalidValueException("displayName must not be blank");
    }
  }

}
