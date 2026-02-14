package com.schemafy.domain.erd.vendor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.vendor.fixture.DbVendorFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DbVendor")
class DbVendorTest {

  @Nested
  @DisplayName("생성 시")
  class Creation {

    @Test
    @DisplayName("유효한 인자로 생성할 수 있다")
    void createsWithValidArgs() {
      var vendor = DbVendorFixture.defaultDbVendor();

      assertThat(vendor.displayName()).isEqualTo(DbVendorFixture.DEFAULT_DISPLAY_NAME);
      assertThat(vendor.name()).isEqualTo(DbVendorFixture.DEFAULT_NAME);
      assertThat(vendor.version()).isEqualTo(DbVendorFixture.DEFAULT_VERSION);
      assertThat(vendor.datatypeMappings()).isEqualTo(DbVendorFixture.DEFAULT_DATATYPE_MAPPINGS);
    }

    @Test
    @DisplayName("displayName이 null이면 예외를 발생시킨다")
    void throwsWhenDisplayNameIsNull() {
      assertThatThrownBy(() -> new DbVendor(null, "mysql", "8.0", "{}"))
          .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("displayName이 공백이면 예외를 발생시킨다")
    void throwsWhenDisplayNameIsBlank() {
      assertThatThrownBy(() -> new DbVendor("  ", "mysql", "8.0", "{}"))
          .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("name이 null이면 예외를 발생시킨다")
    void throwsWhenNameIsNull() {
      assertThatThrownBy(() -> new DbVendor("MySQL 8.0", null, "8.0", "{}"))
          .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("version이 null이면 예외를 발생시킨다")
    void throwsWhenVersionIsNull() {
      assertThatThrownBy(() -> new DbVendor("MySQL 8.0", "mysql", null, "{}"))
          .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("datatypeMappings가 null이면 예외를 발생시킨다")
    void throwsWhenDatatypeMappingsIsNull() {
      assertThatThrownBy(() -> new DbVendor("MySQL 8.0", "mysql", "8.0", null))
          .isInstanceOf(InvalidValueException.class);
    }

  }

}
