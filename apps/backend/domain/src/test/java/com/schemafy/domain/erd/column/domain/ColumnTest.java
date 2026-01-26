package com.schemafy.domain.erd.column.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Column")
class ColumnTest {

  @Nested
  @DisplayName("생성 시")
  class WhenCreating {

    @Test
    @DisplayName("유효한 인자로 생성된다")
    void createsWithValidArguments() {
      var column = ColumnFixture.defaultColumn();

      assertThat(column.id()).isEqualTo(ColumnFixture.DEFAULT_ID);
      assertThat(column.tableId()).isEqualTo(ColumnFixture.DEFAULT_TABLE_ID);
      assertThat(column.name()).isEqualTo(ColumnFixture.DEFAULT_NAME);
      assertThat(column.dataType()).isEqualTo(ColumnFixture.DEFAULT_DATA_TYPE);
      assertThat(column.lengthScale()).isNotNull();
      assertThat(column.lengthScale().length()).isEqualTo(ColumnFixture.DEFAULT_LENGTH);
      assertThat(column.seqNo()).isEqualTo(ColumnFixture.DEFAULT_SEQ_NO);
      assertThat(column.autoIncrement()).isFalse();
      assertThat(column.charset()).isNull();
      assertThat(column.collation()).isNull();
      assertThat(column.comment()).isNull();
    }

    @Test
    @DisplayName("모든 필드가 저장된다")
    void storesAllFields() {
      var lengthScale = new ColumnLengthScale(255, null, null);
      var column = new Column(
          "col-id",
          "tbl-id",
          "test_column",
          "VARCHAR",
          lengthScale,
          5,
          false,
          "utf8mb4",
          "utf8mb4_general_ci",
          "Test comment");

      assertThat(column.id()).isEqualTo("col-id");
      assertThat(column.tableId()).isEqualTo("tbl-id");
      assertThat(column.name()).isEqualTo("test_column");
      assertThat(column.dataType()).isEqualTo("VARCHAR");
      assertThat(column.lengthScale()).isEqualTo(lengthScale);
      assertThat(column.seqNo()).isEqualTo(5);
      assertThat(column.autoIncrement()).isFalse();
      assertThat(column.charset()).isEqualTo("utf8mb4");
      assertThat(column.collation()).isEqualTo("utf8mb4_general_ci");
      assertThat(column.comment()).isEqualTo("Test comment");
    }

    @Test
    @DisplayName("INT 컬럼은 lengthScale 없이 생성된다")
    void createsIntColumnWithoutLengthScale() {
      var column = ColumnFixture.intColumn();

      assertThat(column.dataType()).isEqualTo("INT");
      assertThat(column.lengthScale()).isNull();
    }

    @Test
    @DisplayName("autoIncrement INT 컬럼이 생성된다")
    void createsAutoIncrementIntColumn() {
      var column = ColumnFixture.intColumnWithAutoIncrement();

      assertThat(column.dataType()).isEqualTo("INT");
      assertThat(column.autoIncrement()).isTrue();
    }

    @Test
    @DisplayName("DECIMAL 컬럼은 precision/scale로 생성된다")
    void createsDecimalColumnWithPrecisionScale() {
      var column = ColumnFixture.decimalColumn();

      assertThat(column.dataType()).isEqualTo("DECIMAL");
      assertThat(column.lengthScale()).isNotNull();
      assertThat(column.lengthScale().precision()).isEqualTo(10);
      assertThat(column.lengthScale().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("VARCHAR 컬럼은 charset/collation과 함께 생성된다")
    void createsVarcharColumnWithCharset() {
      var column = ColumnFixture.varcharColumnWithCharset("utf8mb4", "utf8mb4_general_ci");

      assertThat(column.dataType()).isEqualTo("VARCHAR");
      assertThat(column.charset()).isEqualTo("utf8mb4");
      assertThat(column.collation()).isEqualTo("utf8mb4_general_ci");
    }

  }

}
