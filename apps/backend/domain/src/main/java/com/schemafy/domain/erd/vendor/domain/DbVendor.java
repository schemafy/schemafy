package com.schemafy.domain.erd.vendor.domain;

import com.schemafy.domain.common.exception.InvalidValueException;

public record DbVendor(
    String displayName,
    String name,
    String version,
    String datatypeMappings) {

  public DbVendor {
    if (displayName == null || displayName.isBlank())
      throw new InvalidValueException("displayName must not be blank");
    if (name == null || name.isBlank())
      throw new InvalidValueException("name must not be blank");
    if (version == null || version.isBlank())
      throw new InvalidValueException("version must not be blank");
    if (datatypeMappings == null || datatypeMappings.isBlank())
      throw new InvalidValueException("datatypeMappings must not be blank");
  }

}
