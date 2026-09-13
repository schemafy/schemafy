package com.schemafy.core.erd.ddl.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;

public record DdlExportVendor(String value) {

  private static final Pattern VALID_VENDOR = Pattern.compile("[a-z][a-z0-9_-]{0,31}");

  public static final DdlExportVendor MYSQL = new DdlExportVendor("mysql");

  public DdlExportVendor {
    if (value == null || value.isBlank()) {
      throw new DomainException(DdlErrorCode.INVALID_VALUE,
          "DDL export target DB vendor is required");
    }
    value = value.trim().toLowerCase(Locale.ROOT);
    if (!VALID_VENDOR.matcher(value).matches()) {
      throw new DomainException(DdlErrorCode.INVALID_VALUE,
          "Invalid DDL export target DB vendor: " + value);
    }
  }

  public static DdlExportVendor of(String value) {
    return new DdlExportVendor(value);
  }

}
