package com.schemafy.core.erd.vendor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IdentifierCapabilities")
class IdentifierCapabilitiesTest {

  private static final IdentifierCapabilities MYSQL = IdentifierCapabilities.codePoints(64);

  @Test
  @DisplayName("UTF-16 길이가 아닌 Unicode code point 수로 길이를 검증한다")
  void validatesUnicodeCodePointLength() {
    assertThat(MYSQL.allows("😀".repeat(64))).isTrue();
    assertThat(MYSQL.allows("😀".repeat(65))).isFalse();
  }

  @Test
  @DisplayName("자동 이름 suffix 공간을 예약해 최대 길이에 맞춘다")
  void reservesGeneratedNameSuffixWithinLimit() {
    String base = "a".repeat(64);

    assertThat(MYSQL.fitGeneratedName(base, "")).isEqualTo(base);
    assertThat(MYSQL.fitGeneratedName(base, "_1"))
        .isEqualTo("a".repeat(62) + "_1");
    assertThat(MYSQL.fitGeneratedName(base, "_10"))
        .isEqualTo("a".repeat(61) + "_10");
  }

  @Test
  @DisplayName("자동 이름을 자를 때 surrogate pair를 보존한다")
  void preservesCodePointsWhenFittingGeneratedName() {
    String fitted = MYSQL.fitGeneratedName("😀".repeat(64), "_1");

    assertThat(fitted).endsWith("_1");
    assertThat(fitted.codePointCount(0, fitted.length())).isEqualTo(64);
  }

  @Test
  @DisplayName("UTF-8 byte 정책으로 길이를 검증하고 문자 경계를 보존해 자른다")
  void validatesAndFitsUtf8ByteLength() {
    IdentifierCapabilities utf8Bytes = new IdentifierCapabilities(
        10,
        IdentifierLengthUnit.UTF8_BYTES);

    assertThat(utf8Bytes.allows("😀😀")).isTrue();
    assertThat(utf8Bytes.allows("😀😀😀")).isFalse();
    assertThat(utf8Bytes.fitGeneratedName("😀😀😀", "_1"))
        .isEqualTo("😀😀_1");
  }

  @Test
  @DisplayName("entity 고유 제한이 vendor 제한보다 작으면 더 엄격한 제한을 적용한다")
  void appliesStricterLocalLimit() {
    assertThat(MYSQL.allows("a".repeat(40), 40)).isTrue();
    assertThat(MYSQL.allows("a".repeat(41), 40)).isFalse();
    assertThat(MYSQL.fitGeneratedName("a".repeat(40), "_1", 40))
        .isEqualTo("a".repeat(38) + "_1");
  }

  @Test
  @DisplayName("vendor byte 제한과 entity code point 제한을 각각 적용한다")
  void appliesVendorAndLocalLimitsWithTheirOwnUnits() {
    IdentifierCapabilities utf8Bytes = new IdentifierCapabilities(
        8,
        IdentifierLengthUnit.UTF8_BYTES);

    assertThat(utf8Bytes.allows("가나다", 2)).isFalse();
    assertThat(utf8Bytes.fitGeneratedName("가나다", "_1", 3))
        .isEqualTo("가_1");
  }

  @Test
  @DisplayName("최대 길이는 양수여야 한다")
  void rejectsInvalidMaxLength() {
    assertThatThrownBy(() -> IdentifierCapabilities.codePoints(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("길이 측정 단위는 필수다")
  void rejectsMissingLengthUnit() {
    assertThatThrownBy(() -> new IdentifierCapabilities(64, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

}
