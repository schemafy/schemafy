package com.schemafy.core.erd.vendor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

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

      assertThat(vendor.id()).isEqualTo(DbVendorFixture.DEFAULT_ID);
      assertThat(vendor.displayName()).isEqualTo(DbVendorFixture.DEFAULT_DISPLAY_NAME);
      assertThat(vendor.name()).isEqualTo(DbVendorFixture.DEFAULT_NAME);
      assertThat(vendor.version()).isEqualTo(DbVendorFixture.DEFAULT_VERSION);
      assertThat(vendor.datatypeMappings()).isEqualTo(DbVendorFixture.DEFAULT_DATATYPE_MAPPINGS);
      assertThat(vendor.capabilities()).isEqualTo(DbVendorFixture.defaultCapabilities());
    }

    @Test
    @DisplayName("id가 null이면 예외를 발생시킨다")
    void throwsWhenIdIsNull() {
      assertThatThrownBy(() -> new DbVendor(
          null, "MySQL 8.0", "mysql", "8.0", "{}", DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("id가 양수가 아니면 예외를 발생시킨다")
    void throwsWhenIdIsNotPositive() {
      assertThatThrownBy(() -> new DbVendor(
          0, "MySQL 8.0", "mysql", "8.0", "{}", DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("displayName이 null이면 예외를 발생시킨다")
    void throwsWhenDisplayNameIsNull() {
      assertThatThrownBy(() -> new DbVendor(
          DbVendorFixture.DEFAULT_ID, null, "mysql", "8.0", "{}",
          DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("displayName이 공백이면 예외를 발생시킨다")
    void throwsWhenDisplayNameIsBlank() {
      assertThatThrownBy(() -> new DbVendor(
          DbVendorFixture.DEFAULT_ID, "  ", "mysql", "8.0", "{}",
          DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("name이 null이면 예외를 발생시킨다")
    void throwsWhenNameIsNull() {
      assertThatThrownBy(() -> new DbVendor(
          DbVendorFixture.DEFAULT_ID, "MySQL 8.0", null, "8.0", "{}",
          DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("version이 null이면 예외를 발생시킨다")
    void throwsWhenVersionIsNull() {
      assertThatThrownBy(() -> new DbVendor(
          DbVendorFixture.DEFAULT_ID, "MySQL 8.0", "mysql", null, "{}",
          DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("datatypeMappings가 null이면 예외를 발생시킨다")
    void throwsWhenDatatypeMappingsIsNull() {
      assertThatThrownBy(() -> new DbVendor(
          DbVendorFixture.DEFAULT_ID, "MySQL 8.0", "mysql", "8.0", null,
          DbVendorFixture.defaultCapabilities()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("capabilities가 null이면 예외를 발생시킨다")
    void throwsWhenCapabilitiesIsNull() {
      assertThatThrownBy(() -> new DbVendor(
          DbVendorFixture.DEFAULT_ID, "MySQL 8.0", "mysql", "8.0", "{}", null))
          .isInstanceOf(DomainException.class);
    }

  }

}
