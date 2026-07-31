package com.schemafy.core.erd.vendor.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IdentifierValidator")
class IdentifierValidatorTest {

  private static final IdentifierCapabilities MYSQL = IdentifierCapabilities.codePoints(64);

  @Test
  @DisplayName("vendor 제한을 넘은 이름은 entity 오류 코드와 조치 가능한 메시지로 거부한다")
  void rejectsOverlongIdentifierWithEntityErrorCode() {
    assertThatThrownBy(() -> IdentifierValidator.validateLength(
        MYSQL,
        "a".repeat(65),
        SchemaErrorCode.INVALID_VALUE,
        "Schema name"))
        .isInstanceOfSatisfying(DomainException.class, exception -> {
          org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
              .isEqualTo(SchemaErrorCode.INVALID_VALUE);
          org.assertj.core.api.Assertions.assertThat(exception.getMessage())
              .contains("Schema name", "64", "project's DB vendor");
        });
  }

}
