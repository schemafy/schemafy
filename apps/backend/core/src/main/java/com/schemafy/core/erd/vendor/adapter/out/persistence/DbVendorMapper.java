package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;

@Component
class DbVendorMapper {

  private final JsonCodec jsonCodec;

  DbVendorMapper(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

  DbVendor toDomain(DbVendorEntity entity) {
    return new DbVendor(
        entity.getDisplayName(),
        entity.getName(),
        entity.getVersion(),
        jsonCodec.normalizePersistedJson(entity.getDatatypeMappings()));
  }

  DbVendorSummary toSummary(DbVendorEntity entity) {
    return new DbVendorSummary(
        entity.getDisplayName(),
        entity.getName(),
        entity.getVersion());
  }

}
