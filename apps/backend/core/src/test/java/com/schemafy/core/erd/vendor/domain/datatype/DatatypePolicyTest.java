package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.index.domain.type.IndexType;

import static com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicyFixture.definition;
import static com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicyFixture.integerParameter;
import static com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicyFixture.mysqlPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DatatypePolicy")
class DatatypePolicyTest {

  private static final DatatypeValidationErrorCodes COLUMN_ERROR_CODES = new DatatypeValidationErrorCodes(
      ColumnErrorCode.DATA_TYPE_INVALID,
      ColumnErrorCode.LENGTH_REQUIRED,
      ColumnErrorCode.PRECISION_REQUIRED,
      ColumnErrorCode.INVALID_VALUE,
      ColumnErrorCode.AUTO_INCREMENT_NOT_ALLOWED,
      ColumnErrorCode.CHARSET_NOT_ALLOWED);

  @Test
  @DisplayName("schemaVersion 2만 허용한다")
  void rejectsUnsupportedSchemaVersion() {
    assertThatThrownBy(() -> new DatatypePolicy(
        1,
        "mysql",
        null,
        ">= 8.0 < 9.0",
        List.of(mysqlPolicy().types().getFirst())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("schemaVersion");
  }

  @Test
  @DisplayName("canonical 타입과 alias의 대소문자 무시 충돌을 거부한다")
  void rejectsCanonicalAndAliasCollisionCaseInsensitively() {
    DatatypeDefinition intType = definition(
        "INT",
        List.of("INTEGER"),
        List.of(),
        "INT",
        properties());
    DatatypeDefinition collidingType = definition(
        "integer",
        List.of(),
        List.of(),
        "INTEGER",
        properties());

    assertThatThrownBy(() -> new DatatypePolicy(
        2,
        "mysql",
        null,
        ">= 8.0 < 9.0",
        List.of(intType, collidingType)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("collision");
  }

  @Test
  @DisplayName("정확한 버전 또는 비교식 범위 중 하나만 선언한다")
  void requiresExactlyOneVersionSelector() {
    assertThatThrownBy(() -> new DatatypePolicy(
        2,
        "mysql",
        "8.0",
        ">= 8.0 < 9.0",
        List.of(mysqlPolicy().types().getFirst())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly one");
  }

  @Test
  @DisplayName("점으로 구분한 MySQL 버전을 비교식 범위와 대조한다")
  void matchesConjunctiveVersionRange() {
    DatatypePolicy policy = mysqlPolicy();

    assertThat(policy.matchesVersion("8.0")).isTrue();
    assertThat(policy.matchesVersion("8.4.3")).isTrue();
    assertThat(policy.matchesVersion("7.9")).isFalse();
    assertThat(policy.matchesVersion("9.0")).isFalse();
  }

  @Test
  @DisplayName("alias 입력을 canonical 타입 정의로 해석한다")
  void resolvesAliasToCanonicalDefinition() {
    DatatypeDefinition definition = mysqlPolicy().find(" numeric ").orElseThrow();

    assertThat(definition.sqlType()).isEqualTo("DECIMAL");
  }

  @Test
  @DisplayName("DECIMAL은 precision만 지정할 수 있다")
  void validatesDecimalPrecisionWithoutScale() {
    DatatypeDefinition definition = mysqlPolicy().validate(
        "DECIMAL",
        new ColumnTypeArguments(null, 10, null, null),
        false,
        null,
        null,
        COLUMN_ERROR_CODES);

    assertThat(definition.render(
        new ColumnTypeArguments(null, 10, null, null),
        value -> value.replace("'", "''")))
        .isEqualTo("DECIMAL(10)");
  }

  @Test
  @DisplayName("required 인자와 MySQL 범위를 검증한다")
  void validatesRequiredArgumentsAndBounds() {
    assertThatThrownBy(() -> mysqlPolicy().validate(
        "VARCHAR", null, false, null, null, COLUMN_ERROR_CODES))
        .matches(DomainException.hasErrorCode(ColumnErrorCode.LENGTH_REQUIRED));

    assertThatThrownBy(() -> mysqlPolicy().validate(
        "CHAR", new ColumnTypeArguments(256, null, null, null), false, null, null,
        COLUMN_ERROR_CODES))
        .matches(DomainException.hasErrorCode(ColumnErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("AUTO_INCREMENT와 charset/collation 속성을 정책으로 검증한다")
  void validatesDatatypeProperties() {
    assertThatThrownBy(() -> mysqlPolicy().validate(
        "DECIMAL", null, true, null, null, COLUMN_ERROR_CODES))
        .matches(DomainException.hasErrorCode(ColumnErrorCode.AUTO_INCREMENT_NOT_ALLOWED));

    assertThatThrownBy(() -> mysqlPolicy().validate(
        "INT", null, false, "utf8mb4", null, COLUMN_ERROR_CODES))
        .matches(DomainException.hasErrorCode(ColumnErrorCode.CHARSET_NOT_ALLOWED));
  }

  @Test
  @DisplayName("문자열 배열 인자를 MySQL 문자열 리터럴 내용으로 escape해 렌더링한다")
  void rendersEscapedStringArray() {
    ColumnTypeArguments arguments = new ColumnTypeArguments(
        null, null, null, List.of("a", "b's"));

    DatatypeDefinition definition = mysqlPolicy().validate(
        "ENUM", arguments, false, null, null, COLUMN_ERROR_CODES);

    assertThat(definition.render(arguments, value -> value.replace("'", "\\'")))
        .isEqualTo("ENUM('a', 'b\\'s')");
  }

  @Test
  @DisplayName("template의 parameter placeholder 불일치를 거부한다")
  void rejectsUnknownTemplatePlaceholder() {
    assertThatThrownBy(() -> definition(
        "VARCHAR",
        List.of(),
        List.of(integerParameter(DatatypeParameterName.LENGTH, true, 1, 0, 65_535)),
        "VARCHAR({precision})",
        properties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("placeholder");
  }

  private static DatatypeProperties properties() {
    return new DatatypeProperties(false, false, Set.of(IndexType.BTREE), "test");
  }

}
