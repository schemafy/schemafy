package com.schemafy.domain.erd.vendor.domain;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.vendor.domain.exception.VendorErrorCode;

public record DbVendorSummary(
    String displayName,
    String name,
    String version) {

  public DbVendorSummary {
    if (displayName == null || displayName.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "displayName must not be blank");
    if (name == null || name.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "name must not be blank");
    if (version == null || version.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "version must not be blank");
  }

}
