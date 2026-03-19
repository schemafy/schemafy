package com.schemafy.api.erd.service.vendor;

import org.springframework.stereotype.Component;

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
        vendor.displayName(),
        vendor.name(),
        vendor.version(),
        jsonCodec.parseNode(vendor.datatypeMappings()));
  }

}
