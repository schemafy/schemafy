package com.schemafy.domain.erd.schema.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.domain.erd.schema.fixture.SchemaFixture;

@DisplayName("Schema")
class SchemaTest {

  @Nested
  @DisplayName("생성 시")
  class WhenCreating {

    @Test
    @DisplayName("유효한 인자로 생성된다")
    void createsWithValidArguments() {
      var schema = SchemaFixture.defaultSchema();

      assertThat(schema.id()).isEqualTo(SchemaFixture.DEFAULT_ID);
      assertThat(schema.projectId()).isEqualTo(SchemaFixture.DEFAULT_PROJECT_ID);
      assertThat(schema.dbVendorName()).isEqualTo(SchemaFixture.DEFAULT_DB_VENDOR);
      assertThat(schema.name()).isEqualTo(SchemaFixture.DEFAULT_NAME);
      assertThat(schema.charset()).isEqualTo(SchemaFixture.DEFAULT_CHARSET);
      assertThat(schema.collation()).isEqualTo(SchemaFixture.DEFAULT_COLLATION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    @DisplayName("id가 blank이면 예외가 발생한다")
    void throwsWhenIdIsBlank(String invalidId) {
      assertThatThrownBy(() -> new Schema(
          invalidId,
          SchemaFixture.DEFAULT_PROJECT_ID,
          SchemaFixture.DEFAULT_DB_VENDOR,
          SchemaFixture.DEFAULT_NAME,
          SchemaFixture.DEFAULT_CHARSET,
          SchemaFixture.DEFAULT_COLLATION))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    @DisplayName("projectId가 blank이면 예외가 발생한다")
    void throwsWhenProjectIdIsBlank(String invalidProjectId) {
      assertThatThrownBy(() -> new Schema(
          SchemaFixture.DEFAULT_ID,
          invalidProjectId,
          SchemaFixture.DEFAULT_DB_VENDOR,
          SchemaFixture.DEFAULT_NAME,
          SchemaFixture.DEFAULT_CHARSET,
          SchemaFixture.DEFAULT_COLLATION))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("projectId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    @DisplayName("dbVendorName이 blank이면 예외가 발생한다")
    void throwsWhenDbVendorNameIsBlank(String invalidDbVendorName) {
      assertThatThrownBy(() -> new Schema(
          SchemaFixture.DEFAULT_ID,
          SchemaFixture.DEFAULT_PROJECT_ID,
          invalidDbVendorName,
          SchemaFixture.DEFAULT_NAME,
          SchemaFixture.DEFAULT_CHARSET,
          SchemaFixture.DEFAULT_COLLATION))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("dbVendorName");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    @DisplayName("name이 blank이면 예외가 발생한다")
    void throwsWhenNameIsBlank(String invalidName) {
      assertThatThrownBy(() -> new Schema(
          SchemaFixture.DEFAULT_ID,
          SchemaFixture.DEFAULT_PROJECT_ID,
          SchemaFixture.DEFAULT_DB_VENDOR,
          invalidName,
          SchemaFixture.DEFAULT_CHARSET,
          SchemaFixture.DEFAULT_COLLATION))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name");
    }
  }

  @Nested
  @DisplayName("charset이 null 또는 blank일 때")
  class WhenCharsetIsNullOrBlank {

    @ParameterizedTest
    @ValueSource(strings = { "MySQL", "mysql", "MariaDB", "mariadb" })
    @DisplayName("MySQL/MariaDB면 utf8mb4가 기본값이다")
    void defaultsToUtf8mb4ForMySqlFamily(String dbVendor) {
      var schema = SchemaFixture.schemaWithDbVendor(dbVendor);

      assertThat(schema.charset()).isEqualTo("utf8mb4");
    }

    @ParameterizedTest
    @ValueSource(strings = { "PostgreSQL", "Oracle", "SQLServer", "H2" })
    @DisplayName("다른 DB면 utf8이 기본값이다")
    void defaultsToUtf8ForOtherDatabases(String dbVendor) {
      var schema = SchemaFixture.schemaWithDbVendor(dbVendor);

      assertThat(schema.charset()).isEqualTo("utf8");
    }
  }

  @Nested
  @DisplayName("collation이 null 또는 blank일 때")
  class WhenCollationIsNullOrBlank {

    @ParameterizedTest
    @ValueSource(strings = { "MySQL", "mysql", "MariaDB", "mariadb" })
    @DisplayName("MySQL/MariaDB면 utf8mb4_general_ci가 기본값이다")
    void defaultsToUtf8mb4GeneralCiForMySqlFamily(String dbVendor) {
      var schema = SchemaFixture.schemaWithDbVendor(dbVendor);

      assertThat(schema.collation()).isEqualTo("utf8mb4_general_ci");
    }

    @ParameterizedTest
    @ValueSource(strings = { "PostgreSQL", "Oracle", "SQLServer", "H2" })
    @DisplayName("다른 DB면 utf8_general_ci가 기본값이다")
    void defaultsToUtf8GeneralCiForOtherDatabases(String dbVendor) {
      var schema = SchemaFixture.schemaWithDbVendor(dbVendor);

      assertThat(schema.collation()).isEqualTo("utf8_general_ci");
    }
  }

}
