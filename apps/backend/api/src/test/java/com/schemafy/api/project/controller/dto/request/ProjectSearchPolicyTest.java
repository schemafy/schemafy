package com.schemafy.api.project.controller.dto.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.core.common.exception.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectSearchPolicyTest {

  @Test
  void normalizesSearchText() {
    assertThat(ProjectSearchPolicy.normalize("  schema  ")).isEqualTo("schema");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "", " ", "\t", "schema\ntext" })
  void rejectsNullBlankAndControlCharacterSearchText(String search) {
    assertInvalidSearch(search);
  }

  @Test
  void acceptsOneHundredSupplementaryUnicodeCodePoints() {
    String search = "😀".repeat(100);

    assertThat(ProjectSearchPolicy.normalize(search)).isEqualTo(search);
  }

  @Test
  void rejectsSearchTextOverOneHundredUnicodeCodePoints() {
    assertInvalidSearch("😀".repeat(101));
  }

  private void assertInvalidSearch(String search) {
    assertThatThrownBy(() -> ProjectSearchPolicy.normalize(search))
        .isInstanceOf(DomainException.class)
        .hasMessage("search must contain 1 to 100 characters without control characters")
        .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
            .isEqualTo(CommonErrorCode.INVALID_PARAMETER));
  }

}
