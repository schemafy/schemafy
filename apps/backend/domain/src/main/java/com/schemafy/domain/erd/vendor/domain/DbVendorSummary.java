package com.schemafy.domain.erd.vendor.domain;

import com.schemafy.domain.common.exception.InvalidValueException;

public record DbVendorSummary(
    String displayName,
    String name,
    String version) {

  public DbVendorSummary {
    if (displayName == null || displayName.isBlank())
      throw new InvalidValueException("displayName must not be blank");
    if (name == null || name.isBlank())
      throw new InvalidValueException("name must not be blank");
    if (version == null || version.isBlank())
      throw new InvalidValueException("version must not be blank");
  }

}
