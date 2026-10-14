package com.schemafy.api.erd.service.vendor;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.erd.fixture.DbVendorApiFixture;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbVendorApiResponseMapper")
class DbVendorApiResponseMapperTest {

  private static final Integer DB_VENDOR_ID = 1;

  private final DbVendorApiResponseMapper sut = new DbVendorApiResponseMapper();

  @Test
  @DisplayName("typed datatypeMappings를 그대로 반환한다")
  void toDbVendorDetailResponse_mapsTypedDatatypePolicy() {
    DbVendor vendor = new DbVendor(
        DB_VENDOR_ID,
        "MySQL 8.0",
        "mysql",
        "8.0",
        DbVendorApiFixture.mysqlDatatypePolicy(),
        mysqlCapabilities());

    var response = sut.toDbVendorDetailResponse(vendor);

    assertThat(response.id()).isEqualTo(DB_VENDOR_ID);
    assertThat(response.datatypeMappings()).isNotNull();
    assertThat(response.datatypeMappings())
        .isSameAs(vendor.datatypeMappings());
    assertThat(response.datatypeMappings().schemaVersion()).isEqualTo(2);
    assertThat(response.capabilities()).isEqualTo(mysqlCapabilities());
  }

  private static VendorCapabilities mysqlCapabilities() {
    return new VendorCapabilities(
        2,
        new IndexCapabilities(
            Set.of(IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL),
            Set.of(IndexType.BTREE)),
        IdentifierCapabilities.codePoints(64));
  }

}
