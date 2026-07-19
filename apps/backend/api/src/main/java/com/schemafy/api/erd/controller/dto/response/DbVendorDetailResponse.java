package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;

public record DbVendorDetailResponse(
    Integer id,
    String displayName,
    String name,
    String version,
    JsonNode datatypeMappings,
    VendorCapabilities capabilities) {
}
