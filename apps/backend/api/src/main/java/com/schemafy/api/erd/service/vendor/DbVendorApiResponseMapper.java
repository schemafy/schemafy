package com.schemafy.api.erd.service.vendor;

import org.springframework.stereotype.Component;

import com.schemafy.api.erd.controller.dto.response.DbVendorDetailResponse;
import com.schemafy.core.erd.vendor.domain.DbVendor;

@Component
public class DbVendorApiResponseMapper {

  public DbVendorDetailResponse toDbVendorDetailResponse(DbVendor vendor) {
    return new DbVendorDetailResponse(
        vendor.id(),
        vendor.displayName(),
        vendor.name(),
        vendor.version(),
        vendor.datatypeMappings(),
        vendor.capabilities());
  }

}
