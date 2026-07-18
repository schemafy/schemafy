package com.schemafy.core.erd.vendor.fixture;

import java.util.Set;

import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;

public class DbVendorFixture {

  public static final Integer DEFAULT_ID = 1;
  public static final String DEFAULT_DISPLAY_NAME = "MySQL 8.0";
  public static final String DEFAULT_NAME = "mysql";
  public static final String DEFAULT_VERSION = "8.0";
  public static final String DEFAULT_DATATYPE_MAPPINGS = """
      {"schemaVersion":1,"vendor":"mysql","types":[{"sqlType":"INT","displayName":"INT","category":"numeric_integer","parameters":[]}]}""";

  public static DbVendor defaultDbVendor() {
    return new DbVendor(
        DEFAULT_ID,
        DEFAULT_DISPLAY_NAME,
        DEFAULT_NAME,
        DEFAULT_VERSION,
        DEFAULT_DATATYPE_MAPPINGS,
        defaultCapabilities());
  }

  public static VendorCapabilities defaultCapabilities() {
    return new VendorCapabilities(
        1,
        new IndexCapabilities(
            Set.of(IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL),
            Set.of(IndexType.BTREE)));
  }

  public static DbVendorSummary defaultDbVendorSummary() {
    return new DbVendorSummary(
        DEFAULT_ID,
        DEFAULT_DISPLAY_NAME,
        DEFAULT_NAME,
        DEFAULT_VERSION);
  }

  public static GetDbVendorQuery getQuery() { return new GetDbVendorQuery(DEFAULT_ID); }

  public static GetDbVendorQuery getQuery(Integer id) {
    return new GetDbVendorQuery(id);
  }

  private DbVendorFixture() {}

}
