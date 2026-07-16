package com.schemafy.api.erd.service.vendor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.DbVendor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbVendorApiResponseMapper")
class DbVendorApiResponseMapperTest {

  private static final String DB_VENDOR_ID = "01JQ7Z5V6Y8X9W0T1S2R3Q4P5N";

  private final DbVendorApiResponseMapper sut = new DbVendorApiResponseMapper(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @Test
  @DisplayName("datatypeMappings 문자열을 JSON 객체로 변환한다")
  void toDbVendorDetailResponse_mapsDatatypeMappingsJson() {
    DbVendor vendor = new DbVendor(
        DB_VENDOR_ID,
        "MySQL 8.0",
        "mysql",
        "8.0",
        "{\"schemaVersion\":1,\"vendor\":\"mysql\"}");

    var response = sut.toDbVendorDetailResponse(vendor);

    assertThat(response.id()).isEqualTo(DB_VENDOR_ID);
    assertThat(response.datatypeMappings()).isNotNull();
    assertThat(response.datatypeMappings().get("schemaVersion").intValue())
        .isEqualTo(1);
  }

}
