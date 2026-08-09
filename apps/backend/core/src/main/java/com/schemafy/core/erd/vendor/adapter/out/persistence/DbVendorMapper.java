package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;

@Component
class DbVendorMapper {

  private final JsonCodec jsonCodec;

  DbVendorMapper(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

  DbVendor toDomain(DbVendorEntity entity) {
    DatatypePolicy datatypePolicy = toDatatypePolicy(entity);
    return new DbVendor(
        entity.getId(),
        entity.getDisplayName(),
        entity.getName(),
        entity.getVersion(),
        datatypePolicy,
        jsonCodec.fromPersistedJson(entity.getCapabilities(), VendorCapabilities.class));
  }

  private DatatypePolicy toDatatypePolicy(DbVendorEntity entity) {
    try {
      DatatypePolicy policy = jsonCodec.fromPersistedJson(
          entity.getDatatypeMappings(),
          DatatypePolicy.class);
      if (policy == null) {
        throw new IllegalArgumentException("Datatype policy must not be null");
      }
      policy.validateIdentity(entity.getName(), entity.getVersion());
      return policy;
    } catch (RuntimeException exception) {
      throw new DomainException(
          VendorErrorCode.INVALID_DATATYPE_POLICY,
          "Invalid datatype policy for vendor %s %s (id=%s): %s".formatted(
              entity.getName(),
              entity.getVersion(),
              entity.getId(),
              exception.getMessage()));
    }
  }

  DbVendorSummary toSummary(DbVendorEntity entity) {
    return new DbVendorSummary(
        entity.getId(),
        entity.getDisplayName(),
        entity.getName(),
        entity.getVersion());
  }

}
