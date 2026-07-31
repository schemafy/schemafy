package com.schemafy.core.erd.vendor.domain;

import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;

public record VendorCapabilities(
    int schemaVersion,
    IndexCapabilities indexes,
    IdentifierCapabilities identifiers) {

  public VendorCapabilities {
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be positive");
    }
    if (indexes == null) {
      throw new IllegalArgumentException("indexes must not be null");
    }
    if (identifiers == null) {
      throw new IllegalArgumentException("identifiers must not be null");
    }
  }

}
