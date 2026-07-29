package com.schemafy.core.erd.vendor.domain;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;

public record DbVendor(
    Integer id,
    String displayName,
    String name,
    String version,
    String datatypeMappings,
    VendorCapabilities capabilities) {

  public DbVendor {
    if (id == null || id <= 0)
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "id must be positive");
    if (displayName == null || displayName.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "displayName must not be blank");
    if (name == null || name.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "name must not be blank");
    if (version == null || version.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "version must not be blank");
    if (datatypeMappings == null || datatypeMappings.isBlank())
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "datatypeMappings must not be blank");
    if (capabilities == null)
      throw new DomainException(VendorErrorCode.INVALID_VALUE, "capabilities must not be null");
  }

}
