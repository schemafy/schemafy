package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.DbVendor;

public record DbVendorDetailResponse(
    String displayName,
    String name,
    String version,
    JsonNode datatypeMappings) {

  public static DbVendorDetailResponse from(DbVendor vendor,
      JsonCodec jsonCodec) {
    return new DbVendorDetailResponse(
        vendor.displayName(),
        vendor.name(),
        vendor.version(),
        jsonCodec.parseNode(vendor.datatypeMappings()));
  }

}
