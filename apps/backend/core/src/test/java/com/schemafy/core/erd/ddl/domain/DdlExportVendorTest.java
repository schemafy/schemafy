package com.schemafy.core.erd.ddl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DdlExportVendor")
class DdlExportVendorTest {

  @Test
  @DisplayName("입력한 DB vendor 값을 trim/lowercase로 정규화한다")
  void normalizesTargetDbVendor() {
    DdlExportVendor vendor = DdlExportVendor.of(" MySQL ");

    assertThat(vendor.value()).isEqualTo("mysql");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { " ", "mysql;", "my sql", "1mysql" })
  @DisplayName("비어 있거나 안전하지 않은 DB vendor 값이면 예외가 발생한다")
  void throwsWhenTargetDbVendorIsInvalid(String value) {
    assertThatThrownBy(() -> DdlExportVendor.of(value))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

}
