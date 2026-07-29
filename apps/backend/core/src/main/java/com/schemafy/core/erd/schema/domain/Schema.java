package com.schemafy.core.erd.schema.domain;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;

public record Schema(
    String id,
    String projectId,
    String name,
    String charset,
    String collation) {

  public Schema {
    if (id == null || id.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "id must not be blank");
    if (projectId == null || projectId.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "projectId must not be blank");
    if (name == null || name.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "name must not be blank");
    if (charset == null || charset.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "charset must not be blank");
    if (collation == null || collation.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "collation must not be blank");
  }

  public static Schema create(
      String id,
      String projectId,
      String dbVendorName,
      String name,
      String charset,
      String collation) {
    if (dbVendorName == null || dbVendorName.isBlank())
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "dbVendorName must not be blank");
    String resolvedCharset = charset == null || charset.isBlank()
        ? defaultCharset(dbVendorName)
        : charset;
    String resolvedCollation = collation == null || collation.isBlank()
        ? defaultCollation(dbVendorName)
        : collation;
    return new Schema(id, projectId, name, resolvedCharset, resolvedCollation);
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
