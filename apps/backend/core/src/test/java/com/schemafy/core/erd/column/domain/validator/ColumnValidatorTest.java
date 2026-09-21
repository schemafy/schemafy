package com.schemafy.core.erd.column.domain.validator;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.fixture.ColumnFixture;

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

    @Test
    @DisplayName("40자 이름은 통과한다")
    void passesAt40CharacterLimit() {
      assertThatCode(() -> ColumnValidator.validateName("a".repeat(40)))
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
  @DisplayName("validateAutoIncrementUniqueness 메서드는")
  class ValidateAutoIncrementUniqueness {

    @Test
    @DisplayName("이미 autoIncrement 컬럼이 있으면 예외가 발생한다")
    void throwsWhenAutoIncrementAlreadyExists() {
      var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrement();
      List<Column> columns = List.of(existingAutoIncrement);

      assertThatThrownBy(() -> ColumnValidator.validateAutoIncrementUniqueness(true, columns, null))
          .matches(DomainException.hasErrorCode(ColumnErrorCode.MULTIPLE_AUTO_INCREMENT))
          .hasMessageContaining("Only one auto-increment column");
    }

    @Test
    @DisplayName("자기 자신은 중복 체크에서 제외된다")
    void ignoresOwnColumnInDuplicateCheck() {
      var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrement();
      List<Column> columns = List.of(existingAutoIncrement);

      assertThatCode(() -> ColumnValidator.validateAutoIncrementUniqueness(
          true, columns, existingAutoIncrement.id()))
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

}
