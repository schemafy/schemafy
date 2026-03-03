package com.schemafy.domain.erd.column.domain.validator;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ColumnValidator")
class ColumnValidatorTest {

  @Nested
  @DisplayName("validateName 메서드는")
  class ValidateName {

    @ParameterizedTest
    @ValueSource(strings = { "name", "column_name", "Col1", "_private", "A" })
    @DisplayName("유효한 이름은 통과한다")
    void passesForValidName(String name) {
      assertThatCode(() -> ColumnValidator.validateName(name))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    @DisplayName("blank이면 예외가 발생한다")
    void throwsWhenBlank(String name) {
      assertThatThrownBy(() -> ColumnValidator.validateName(name))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_INVALID))
          .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("40자 초과하면 예외가 발생한다")
    void throwsWhenExceeds40Characters() {
      String longName = "a".repeat(41);

      assertThatThrownBy(() -> ColumnValidator.validateName(longName))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_INVALID))
          .hasMessageContaining("40");
    }

    @ParameterizedTest
    @ValueSource(strings = { "123col", "col-name", "col.name", "col name" })
    @DisplayName("잘못된 형식이면 예외가 발생한다")
    void throwsWhenInvalidFormat(String name) {
      assertThatThrownBy(() -> ColumnValidator.validateName(name))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_INVALID))
          .hasMessageContaining("invalid format");
    }

  }

  @Nested
  @DisplayName("validateReservedKeyword 메서드는")
  class ValidateReservedKeyword {

    private static final String MYSQL = "mysql";

    @ParameterizedTest
    @ValueSource(strings = { "SELECT", "INSERT", "UPDATE", "DELETE", "FROM", "WHERE",
      "JOIN", "ORDER", "GROUP", "HAVING", "TABLE", "CREATE" })
    @DisplayName("예약어면 예외가 발생한다 (대문자)")
    void throwsForReservedKeywordsUppercase(String keyword) {
      assertThatThrownBy(() -> ColumnValidator.validateReservedKeyword(MYSQL, keyword))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_RESERVED))
          .hasMessageContaining("reserved keyword");
    }

    @ParameterizedTest
    @ValueSource(strings = { "select", "insert", "update", "delete", "from", "where",
      "join", "order", "group", "having", "table", "create" })
    @DisplayName("예약어면 예외가 발생한다 (소문자)")
    void throwsForReservedKeywordsLowercase(String keyword) {
      assertThatThrownBy(() -> ColumnValidator.validateReservedKeyword(MYSQL, keyword))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_RESERVED))
          .hasMessageContaining("reserved keyword");
    }

    @ParameterizedTest
    @ValueSource(strings = { "column_name", "user_select", "order_id", "table_name" })
    @DisplayName("예약어가 아니면 통과한다")
    void passesForNonReservedKeywords(String name) {
      assertThatCode(() -> ColumnValidator.validateReservedKeyword(MYSQL, name))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 이름이면 통과한다")
    void passesForNullName() {
      assertThatCode(() -> ColumnValidator.validateReservedKeyword(MYSQL, null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 벤더면 MySQL 기본값으로 검사한다")
    void usesDefaultVendorWhenNull() {
      assertThatThrownBy(() -> ColumnValidator.validateReservedKeyword(null, "SELECT"))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_RESERVED));
    }

    @ParameterizedTest
    @ValueSource(strings = { "INDEX", "KEY", "PRIMARY", "FOREIGN", "REFERENCES" })
    @DisplayName("확장된 예약어도 검사한다")
    void throwsForExtendedReservedKeywords(String keyword) {
      assertThatThrownBy(() -> ColumnValidator.validateReservedKeyword(MYSQL, keyword))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_RESERVED))
          .hasMessageContaining("reserved keyword");
    }

  }

  @Nested
  @DisplayName("validateNameUniqueness 메서드는")
  class ValidateNameUniqueness {

    @Test
    @DisplayName("중복 이름이 있으면 예외가 발생한다")
    void throwsWhenDuplicateName() {
      var existingColumn = ColumnFixture.columnWithName("existing_column");
      List<Column> columns = List.of(existingColumn);

      assertThatThrownBy(() -> ColumnValidator.validateNameUniqueness(columns, "existing_column", null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_DUPLICATE))
          .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("대소문자 무시하고 중복을 검사한다")
    void ignoresCaseWhenCheckingDuplicates() {
      var existingColumn = ColumnFixture.columnWithName("EXISTING_COLUMN");
      List<Column> columns = List.of(existingColumn);

      assertThatThrownBy(() -> ColumnValidator.validateNameUniqueness(columns, "existing_column", null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.NAME_DUPLICATE));
    }

    @Test
    @DisplayName("자기 자신은 무시한다")
    void ignoresOwnColumn() {
      var existingColumn = ColumnFixture.defaultColumn();
      List<Column> columns = List.of(existingColumn);

      assertThatCode(() -> ColumnValidator.validateNameUniqueness(
          columns, existingColumn.name(), existingColumn.id()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("중복 없으면 통과한다")
    void passesWhenNoDuplicate() {
      var existingColumn = ColumnFixture.columnWithName("existing_column");
      List<Column> columns = List.of(existingColumn);

      assertThatCode(() -> ColumnValidator.validateNameUniqueness(columns, "new_column", null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 목록이면 통과한다")
    void passesWhenEmptyList() {
      assertThatCode(() -> ColumnValidator.validateNameUniqueness(List.of(), "new_column", null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 목록이면 통과한다")
    void passesWhenNullList() {
      assertThatCode(() -> ColumnValidator.validateNameUniqueness(null, "new_column", null))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateDataType 메서드는")
  class ValidateDataType {

    @ParameterizedTest
    @ValueSource(strings = { "INT", "VARCHAR", "TEXT", "DECIMAL", "BOOLEAN", "DATE", "DATETIME", "JSON" })
    @DisplayName("지원하는 타입이면 통과한다")
    void passesForSupportedTypes(String dataType) {
      assertThatCode(() -> ColumnValidator.validateDataType(dataType))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "int", "varchar", "decimal" })
    @DisplayName("소문자 타입도 통과한다")
    void passesForLowercaseTypes(String dataType) {
      assertThatCode(() -> ColumnValidator.validateDataType(dataType))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "UNSUPPORTED", "CUSTOM_TYPE", "NUMBER" })
    @DisplayName("지원하지 않는 타입이면 예외가 발생한다")
    void throwsForUnsupportedTypes(String dataType) {
      assertThatThrownBy(() -> ColumnValidator.validateDataType(dataType))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.DATA_TYPE_INVALID))
          .hasMessageContaining("Unsupported");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t" })
    @DisplayName("blank면 예외가 발생한다")
    void throwsWhenBlank(String dataType) {
      assertThatThrownBy(() -> ColumnValidator.validateDataType(dataType))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.DATA_TYPE_INVALID))
          .hasMessageContaining("blank");
    }

  }

  @Nested
  @DisplayName("validateLengthScale 메서드는")
  class ValidateLengthScale {

    @ParameterizedTest
    @ValueSource(strings = { "VARCHAR", "CHAR" })
    @DisplayName("VARCHAR/CHAR는 length가 필수이다")
    void requiresLengthForVarcharAndChar(String dataType) {
      assertThatThrownBy(() -> ColumnValidator.validateLengthScale(dataType, null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.LENGTH_REQUIRED))
          .hasMessageContaining("Length is required");

      var noLength = new ColumnLengthScale(null, 10, 2);
      assertThatThrownBy(() -> ColumnValidator.validateLengthScale(dataType, noLength))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.LENGTH_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(strings = { "VARCHAR", "CHAR" })
    @DisplayName("VARCHAR/CHAR에 length가 있으면 통과한다")
    void passesWhenLengthProvidedForVarcharAndChar(String dataType) {
      var withLength = new ColumnLengthScale(255, null, null);

      assertThatCode(() -> ColumnValidator.validateLengthScale(dataType, withLength))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "DECIMAL", "NUMERIC" })
    @DisplayName("DECIMAL/NUMERIC은 precision/scale이 필수이다")
    void requiresPrecisionScaleForDecimalAndNumeric(String dataType) {
      assertThatThrownBy(() -> ColumnValidator.validateLengthScale(dataType, null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.PRECISION_REQUIRED))
          .hasMessageContaining("Precision and scale are required");

      var noScale = new ColumnLengthScale(255, null, null);
      assertThatThrownBy(() -> ColumnValidator.validateLengthScale(dataType, noScale))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.PRECISION_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(strings = { "DECIMAL", "NUMERIC" })
    @DisplayName("DECIMAL/NUMERIC에 precision/scale이 있으면 통과한다")
    void passesWhenPrecisionScaleProvidedForDecimalAndNumeric(String dataType) {
      var withPrecisionScale = new ColumnLengthScale(null, 10, 2);

      assertThatCode(() -> ColumnValidator.validateLengthScale(dataType, withPrecisionScale))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "INT", "TEXT", "DATE", "BOOLEAN" })
    @DisplayName("다른 타입은 length/precision 없어도 통과한다")
    void passesWithoutLengthForOtherTypes(String dataType) {
      assertThatCode(() -> ColumnValidator.validateLengthScale(dataType, null))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateAutoIncrement 메서드는")
  class ValidateAutoIncrement {

    @ParameterizedTest
    @ValueSource(strings = { "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER", "BIGINT" })
    @DisplayName("정수 타입이면 autoIncrement가 허용된다")
    void allowsAutoIncrementForIntegerTypes(String dataType) {
      assertThatCode(() -> ColumnValidator.validateAutoIncrement(dataType, true, List.of(), null))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "VARCHAR", "TEXT", "DECIMAL", "FLOAT", "DATE", "BOOLEAN" })
    @DisplayName("비정수 타입이면 autoIncrement가 허용되지 않는다")
    void disallowsAutoIncrementForNonIntegerTypes(String dataType) {
      assertThatThrownBy(() -> ColumnValidator.validateAutoIncrement(dataType, true, List.of(), null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.AUTO_INCREMENT_NOT_ALLOWED))
          .hasMessageContaining("only allowed for integer types");
    }

    @Test
    @DisplayName("autoIncrement가 false면 어떤 타입이든 통과한다")
    void passesWhenAutoIncrementIsFalse() {
      assertThatCode(() -> ColumnValidator.validateAutoIncrement("VARCHAR", false, List.of(), null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 autoIncrement 컬럼이 있으면 예외가 발생한다")
    void throwsWhenAutoIncrementAlreadyExists() {
      var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrement();
      List<Column> columns = List.of(existingAutoIncrement);

      assertThatThrownBy(() -> ColumnValidator.validateAutoIncrement("INT", true, columns, null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.MULTIPLE_AUTO_INCREMENT))
          .hasMessageContaining("Only one auto-increment column");
    }

    @Test
    @DisplayName("자기 자신은 중복 체크에서 제외된다")
    void ignoresOwnColumnInDuplicateCheck() {
      var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrement();
      List<Column> columns = List.of(existingAutoIncrement);

      assertThatCode(() -> ColumnValidator.validateAutoIncrement(
          "INT", true, columns, existingAutoIncrement.id()))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateCharsetAndCollation 메서드는")
  class ValidateCharsetAndCollation {

    @ParameterizedTest
    @ValueSource(strings = { "CHAR", "VARCHAR", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "ENUM", "SET" })
    @DisplayName("텍스트 타입이면 charset/collation이 허용된다")
    void allowsCharsetForTextTypes(String dataType) {
      assertThatCode(() -> ColumnValidator.validateCharsetAndCollation(dataType, "utf8mb4", "utf8mb4_general_ci"))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "INT", "DECIMAL", "DATE", "BOOLEAN", "BLOB", "JSON" })
    @DisplayName("비텍스트 타입이면 charset/collation이 허용되지 않는다")
    void disallowsCharsetForNonTextTypes(String dataType) {
      assertThatThrownBy(() -> ColumnValidator.validateCharsetAndCollation(dataType, "utf8mb4", null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.CHARSET_NOT_ALLOWED))
          .hasMessageContaining("only allowed for text types");
    }

    @Test
    @DisplayName("charset/collation이 모두 null이면 어떤 타입이든 통과한다")
    void passesWhenBothNull() {
      assertThatCode(() -> ColumnValidator.validateCharsetAndCollation("INT", null, null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("charset/collation이 모두 blank면 통과한다")
    void passesWhenBothBlank() {
      assertThatCode(() -> ColumnValidator.validateCharsetAndCollation("INT", "  ", "  "))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validatePosition 메서드는")
  class ValidatePosition {

    @Test
    @DisplayName("0 이상이면 통과한다")
    void passesForZeroOrPositive() {
      assertThatCode(() -> ColumnValidator.validatePosition(0))
          .doesNotThrowAnyException();
      assertThatCode(() -> ColumnValidator.validatePosition(1))
          .doesNotThrowAnyException();
      assertThatCode(() -> ColumnValidator.validatePosition(100))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("음수이면 예외가 발생한다")
    void throwsForNegative() {
      assertThatThrownBy(() -> ColumnValidator.validatePosition(-1))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.POSITION_INVALID))
          .hasMessageContaining("zero or positive");
    }

  }

  @Nested
  @DisplayName("normalizeDataType 메서드는")
  class NormalizeDataType {

    @ParameterizedTest
    @ValueSource(strings = { "int", "INT", "Int" })
    @DisplayName("대문자로 정규화한다")
    void normalizesToUppercase(String dataType) {
      var result = ColumnValidator.normalizeDataType(dataType);

      assertThat(result).isEqualTo("INT");
    }

    @Test
    @DisplayName("공백을 제거한다")
    void trimsWhitespace() {
      var result = ColumnValidator.normalizeDataType("  varchar  ");

      assertThat(result).isEqualTo("VARCHAR");
    }

  }

  @Nested
  @DisplayName("isTextType 메서드는")
  class IsTextType {

    @ParameterizedTest
    @ValueSource(strings = { "CHAR", "VARCHAR", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "ENUM", "SET" })
    @DisplayName("텍스트 타입이면 true를 반환한다")
    void returnsTrueForTextTypes(String dataType) {
      assertThat(ColumnValidator.isTextType(dataType)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "char", "varchar", "text", "enum" })
    @DisplayName("소문자 텍스트 타입도 true를 반환한다")
    void returnsTrueForLowercaseTextTypes(String dataType) {
      assertThat(ColumnValidator.isTextType(dataType)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "INT", "BIGINT", "DECIMAL", "FLOAT", "DOUBLE", "DATE", "DATETIME", "BOOLEAN", "BLOB",
      "JSON" })
    @DisplayName("비텍스트 타입이면 false를 반환한다")
    void returnsFalseForNonTextTypes(String dataType) {
      assertThat(ColumnValidator.isTextType(dataType)).isFalse();
    }

    @Test
    @DisplayName("null이면 false를 반환한다")
    void returnsFalseForNull() {
      assertThat(ColumnValidator.isTextType(null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t" })
    @DisplayName("blank면 false를 반환한다")
    void returnsFalseForBlank(String dataType) {
      assertThat(ColumnValidator.isTextType(dataType)).isFalse();
    }

    @Test
    @DisplayName("공백이 있는 타입명은 trim 후 검사한다")
    void trimsWhitespace() {
      assertThat(ColumnValidator.isTextType("  VARCHAR  ")).isTrue();
      assertThat(ColumnValidator.isTextType("  INT  ")).isFalse();
    }

  }

}
