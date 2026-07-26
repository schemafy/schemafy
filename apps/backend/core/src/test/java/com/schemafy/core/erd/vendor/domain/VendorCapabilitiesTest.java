package com.schemafy.core.erd.vendor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VendorCapabilities")
class VendorCapabilitiesTest {

  private final JsonCodec jsonCodec = new JsonCodec(
      new ObjectMapper().findAndRegisterModules());

  @Test
  @DisplayName("저장 JSON에서 타입이 지정된 capability로 복원한다")
  void restoresFromPersistedJson() {
    String rawJson = """
        {"schemaVersion":1,"indexes":{"supportedTypes":["BTREE","FULLTEXT","SPATIAL"],"sortDirectionTypes":["BTREE"]}}
        """;

    VendorCapabilities capabilities = jsonCodec.fromPersistedJson(
        rawJson,
        VendorCapabilities.class);

    assertThat(capabilities).isEqualTo(DbVendorFixture.defaultCapabilities());
    assertThat(capabilities.indexes().supports(IndexType.HASH)).isFalse();
  }

  @Test
  @DisplayName("schemaVersion은 양수여야 한다")
  void rejectsInvalidSchemaVersion() {
    assertThatThrownBy(() -> new VendorCapabilities(
        0,
        DbVendorFixture.defaultCapabilities().indexes()))
        .isInstanceOf(IllegalArgumentException.class);
  }

}
