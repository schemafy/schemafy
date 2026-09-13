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
        {"schemaVersion":2,"indexes":{"supportedTypes":["BTREE","FULLTEXT","SPATIAL"],"sortDirectionTypes":["BTREE"]},"identifiers":{"maxLength":64,"lengthUnit":"CODE_POINTS"}}
        """;

    VendorCapabilities capabilities = jsonCodec.fromPersistedJson(
        rawJson,
        VendorCapabilities.class);

    assertThat(capabilities).isEqualTo(DbVendorFixture.defaultCapabilities());
    assertThat(capabilities.indexes().supports(IndexType.HASH)).isFalse();
    assertThat(capabilities.identifiers().maxLength()).isEqualTo(64);
    assertThat(capabilities.identifiers().lengthUnit())
        .isEqualTo(IdentifierLengthUnit.CODE_POINTS);
  }

  @Test
  @DisplayName("저장된 identifier capability에는 길이 측정 단위가 필요하다")
  void rejectsPersistedIdentifierCapabilitiesWithoutLengthUnit() {
    String rawJson = """
        {"schemaVersion":2,"indexes":{"supportedTypes":["BTREE"],"sortDirectionTypes":["BTREE"]},"identifiers":{"maxLength":64}}
        """;

    assertThatThrownBy(() -> jsonCodec.fromPersistedJson(
        rawJson,
        VendorCapabilities.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to parse JSON");
  }

  @Test
  @DisplayName("schemaVersion은 양수여야 한다")
  void rejectsInvalidSchemaVersion() {
    assertThatThrownBy(() -> new VendorCapabilities(
        0,
        DbVendorFixture.defaultCapabilities().indexes(),
        DbVendorFixture.defaultCapabilities().identifiers()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("identifier capability는 필수다")
  void rejectsMissingIdentifierCapabilities() {
    assertThatThrownBy(() -> new VendorCapabilities(
        2,
        DbVendorFixture.defaultCapabilities().indexes(),
        null))
        .isInstanceOf(IllegalArgumentException.class);
  }

}
