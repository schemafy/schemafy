package com.schemafy.domain.erd.schema.domain;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;

public record Schema(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {

  public Schema {
    if (id == null || id.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "id must not be blank");
    if (projectId == null || projectId.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "projectId must not be blank");
    if (dbVendorName == null || dbVendorName.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "dbVendorName must not be blank");
    if (name == null || name.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "name must not be blank");
    if (charset == null || charset.isBlank())
      charset = defaultCharset(dbVendorName);
    if (collation == null || collation.isBlank())
      collation = defaultCollation(dbVendorName);
  }

  private static String defaultCharset(String dbVendorName) {
    return switch (dbVendorName.toLowerCase()) {
    case "mysql", "mariadb" -> "utf8mb4";
    default -> "utf8";
    };
  }

  private static String defaultCollation(String dbVendorName) {
    return switch (dbVendorName.toLowerCase()) {
    case "mysql", "mariadb" -> "utf8mb4_general_ci";
    default -> "utf8_general_ci";
    };
  }

}
