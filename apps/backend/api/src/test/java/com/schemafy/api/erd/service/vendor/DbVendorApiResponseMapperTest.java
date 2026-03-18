package com.schemafy.api.erd.service.vendor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.DbVendor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbVendorApiResponseMapper")
class DbVendorApiResponseMapperTest {

  private final DbVendorApiResponseMapper sut = new DbVendorApiResponseMapper(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @Test
  @DisplayName("datatypeMappings 문자열을 JSON 객체로 변환한다")
  void toDbVendorDetailResponse_mapsDatatypeMappingsJson() {
    DbVendor vendor = new DbVendor(
        "MySQL 8.0",
        "mysql",
        "8.0",
        "{\"schemaVersion\":1,\"vendor\":\"mysql\"}");

    var response = sut.toDbVendorDetailResponse(vendor);

    assertThat(response.datatypeMappings()).isNotNull();
    assertThat(response.datatypeMappings().get("schemaVersion").intValue())
        .isEqualTo(1);
  }

}
