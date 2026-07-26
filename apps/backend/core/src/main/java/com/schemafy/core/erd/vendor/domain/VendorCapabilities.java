package com.schemafy.core.erd.vendor.domain;

import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;

public record VendorCapabilities(
    int schemaVersion,
    IndexCapabilities indexes) {

  public VendorCapabilities {
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be positive");
    }
    if (indexes == null) {
      throw new IllegalArgumentException("indexes must not be null");
    }
  }

}
