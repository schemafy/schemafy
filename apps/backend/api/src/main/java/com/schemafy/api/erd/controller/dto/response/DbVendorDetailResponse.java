package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record DbVendorDetailResponse(
    Long id,
    String displayName,
    String name,
    String version,
    JsonNode datatypeMappings) {
}
