package com.schemafy.api.erd.service.vendor;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.erd.controller.dto.response.DbVendorDetailResponse;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.DbVendor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbVendorApiResponseMapper {

  private final JsonCodec jsonCodec;

  public DbVendorDetailResponse toDbVendorDetailResponse(DbVendor vendor) {
    return new DbVendorDetailResponse(
        vendor.id(),
        vendor.displayName(),
        vendor.name(),
        vendor.version(),
        jsonCodec.fromJson(vendor.datatypeMappings(), JsonNode.class));
  }

}
