package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;

@Component
class DbVendorMapper {

  DbVendor toDomain(DbVendorEntity entity) {
    return new DbVendor(
        entity.getDisplayName(),
        entity.getName(),
        entity.getVersion(),
        entity.getDatatypeMappings());
  }

  DbVendorSummary toSummary(DbVendorEntity entity) {
    return new DbVendorSummary(
        entity.getDisplayName(),
        entity.getName(),
        entity.getVersion());
  }

}
