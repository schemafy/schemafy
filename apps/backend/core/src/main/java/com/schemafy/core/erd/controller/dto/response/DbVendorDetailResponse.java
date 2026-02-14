package com.schemafy.core.erd.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.schemafy.domain.erd.vendor.domain.DbVendor;

public record DbVendorDetailResponse(
    String displayName,
    String name,
    String version,
    @JsonRawValue String datatypeMappings) {

  public static DbVendorDetailResponse from(DbVendor vendor) {
    return new DbVendorDetailResponse(
        vendor.displayName(),
        vendor.name(),
        vendor.version(),
        vendor.datatypeMappings());
  }

}
