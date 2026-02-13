package com.schemafy.domain.erd.vendor.fixture;

import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.domain.erd.vendor.domain.DbVendor;
import com.schemafy.domain.erd.vendor.domain.DbVendorSummary;

public class DbVendorFixture {

  public static final String DEFAULT_DISPLAY_NAME = "MySQL 8.0";
  public static final String DEFAULT_NAME = "mysql";
  public static final String DEFAULT_VERSION = "8.0";
  public static final String DEFAULT_DATATYPE_MAPPINGS = """
      {"schemaVersion":1,"vendor":"mysql","types":[{"sqlType":"INT","displayName":"INT","category":"numeric_integer","parameters":[]}]}""";

  public static DbVendor defaultDbVendor() {
    return new DbVendor(
        DEFAULT_DISPLAY_NAME,
        DEFAULT_NAME,
        DEFAULT_VERSION,
        DEFAULT_DATATYPE_MAPPINGS);
  }

  public static DbVendorSummary defaultDbVendorSummary() {
    return new DbVendorSummary(
        DEFAULT_DISPLAY_NAME,
        DEFAULT_NAME,
        DEFAULT_VERSION);
  }

  public static GetDbVendorQuery getQuery() { return new GetDbVendorQuery(DEFAULT_DISPLAY_NAME); }

  public static GetDbVendorQuery getQuery(String displayName) {
    return new GetDbVendorQuery(displayName);
  }

  private DbVendorFixture() {}

}
