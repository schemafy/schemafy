package com.schemafy.core.common.persistence;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlLikePattern")
class SqlLikePatternTest {

  @ParameterizedTest
  @MethodSource("containsCases")
  @DisplayName("contains 패턴을 소문자화하고 LIKE 특수문자를 escape한다")
  void containsEscapesLikeSpecialCharacters(String input, String expected) {
    assertThat(SqlLikePattern.contains(input)).isEqualTo(expected);
  }

  private static Stream<Arguments> containsCases() {
    return Stream.of(
        Arguments.of("Schema", "%schema%"),
        Arguments.of("100%_Real", "%100!%!_real%"),
        Arguments.of("A!%_\\", "%a!!!%!_\\%"));
  }

  @Test
  @DisplayName("기본 locale과 무관하게 ROOT locale으로 소문자화한다")
  void lowercasesIndependentlyOfDefaultLocale() {
    Locale previousDefault = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));

      assertThat(SqlLikePattern.contains("I")).isEqualTo("%i%");
    } finally {
      Locale.setDefault(previousDefault);
    }
  }

}
