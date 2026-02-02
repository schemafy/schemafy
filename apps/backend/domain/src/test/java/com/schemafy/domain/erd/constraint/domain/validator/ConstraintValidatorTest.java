package com.schemafy.domain.erd.constraint.domain.validator;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintDefinitionDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintExpressionRequiredException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameInvalidException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.constraint.domain.exception.MultiplePrimaryKeyConstraintException;
import com.schemafy.domain.erd.constraint.domain.exception.UniqueSameAsPrimaryKeyException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConstraintValidator")
class ConstraintValidatorTest {

  @Nested
  @DisplayName("validateName 메서드는")
  class ValidateName {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null이거나 빈 문자열이면 예외가 발생한다")
    void throwsWhenNullOrEmpty(String name) {
      assertThatThrownBy(() -> ConstraintValidator.validateName(name))
          .isInstanceOf(ConstraintNameInvalidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "   ", "\t", "\n" })
    @DisplayName("공백 문자열이면 예외가 발생한다")
    void throwsWhenBlank(String name) {
      assertThatThrownBy(() -> ConstraintValidator.validateName(name))
          .isInstanceOf(ConstraintNameInvalidException.class);
    }

    @Test
    @DisplayName("256자 이상이면 예외가 발생한다")
    void throwsWhenTooLong() {
      String longName = "a".repeat(256);

      assertThatThrownBy(() -> ConstraintValidator.validateName(longName))
          .isInstanceOf(ConstraintNameInvalidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "pk_constraint", "PK_users_id" })
    @DisplayName("유효한 이름이면 통과한다")
    void passesWithValidName(String name) {
      assertThatCode(() -> ConstraintValidator.validateName(name))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("255자 이름은 통과한다")
    void passesWithMaxLengthName() {
      String maxLengthName = "a".repeat(255);

      assertThatCode(() -> ConstraintValidator.validateName(maxLengthName))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validatePosition 메서드는")
  class ValidatePosition {

    @Test
    @DisplayName("음수면 예외가 발생한다")
    void throwsWhenNegative() {
      assertThatThrownBy(() -> ConstraintValidator.validatePosition(-1))
          .isInstanceOf(ConstraintPositionInvalidException.class);
    }

    @Test
    @DisplayName("0이면 통과한다")
    void passesWithZero() {
      assertThatCode(() -> ConstraintValidator.validatePosition(0))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("양수면 통과한다")
    void passesWithPositive() {
      assertThatCode(() -> ConstraintValidator.validatePosition(5))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateSeqNoIntegrity 메서드는")
  class ValidateSeqNoIntegrity {

    @Test
    @DisplayName("seqNo가 음수면 예외가 발생한다")
    void throwsWhenNegative() {
      List<Integer> seqNos = List.of(-1, 0);

      assertThatThrownBy(() -> ConstraintValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(ConstraintPositionInvalidException.class);
    }

    @Test
    @DisplayName("seqNo가 중복되면 예외가 발생한다")
    void throwsWhenDuplicate() {
      List<Integer> seqNos = List.of(0, 0);

      assertThatThrownBy(() -> ConstraintValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(ConstraintPositionInvalidException.class);
    }

    @Test
    @DisplayName("seqNo가 0부터 연속되지 않으면 예외가 발생한다")
    void throwsWhenNotContiguous() {
      List<Integer> seqNos = List.of(0, 2);

      assertThatThrownBy(() -> ConstraintValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(ConstraintPositionInvalidException.class);
    }

    @Test
    @DisplayName("seqNo가 0부터 시작하지 않으면 예외가 발생한다")
    void throwsWhenNotStartingFromZero() {
      List<Integer> seqNos = List.of(1, 2);

      assertThatThrownBy(() -> ConstraintValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(ConstraintPositionInvalidException.class);
    }

    @Test
    @DisplayName("유효한 seqNo 리스트면 통과한다")
    void passesWithValidSeqNos() {
      List<Integer> seqNos = List.of(0, 1, 2);

      assertThatCode(() -> ConstraintValidator.validateSeqNoIntegrity(seqNos))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null이나 빈 리스트면 통과한다")
    void passesWithNullOrEmpty() {
      assertThatCode(() -> ConstraintValidator.validateSeqNoIntegrity(null))
          .doesNotThrowAnyException();
      assertThatCode(() -> ConstraintValidator.validateSeqNoIntegrity(List.of()))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateColumnExistence 메서드는")
  class ValidateColumnExistence {

    @Test
    @DisplayName("컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExists() {
      List<Column> tableColumns = List.of(ColumnFixture.columnWithId("col1"));
      List<String> columnIds = List.of("col1", "col2");

      assertThatThrownBy(() -> ConstraintValidator.validateColumnExistence(
          tableColumns, columnIds, "test_constraint"))
          .isInstanceOf(ConstraintColumnNotExistException.class)
          .hasMessageContaining("col2");
    }

    @Test
    @DisplayName("모든 컬럼이 존재하면 통과한다")
    void passesWhenAllColumnsExist() {
      List<Column> tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));
      List<String> columnIds = List.of("col1", "col2");

      assertThatCode(() -> ConstraintValidator.validateColumnExistence(
          tableColumns, columnIds, "test_constraint"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("대소문자 무시하고 비교한다")
    void ignoresCaseWhenComparing() {
      List<Column> tableColumns = List.of(ColumnFixture.columnWithId("COL1"));
      List<String> columnIds = List.of("col1");

      assertThatCode(() -> ConstraintValidator.validateColumnExistence(
          tableColumns, columnIds, "test_constraint"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 값이면 통과한다")
    void passesWithNullValues() {
      assertThatCode(() -> ConstraintValidator.validateColumnExistence(
          null, List.of("col1"), "test"))
          .doesNotThrowAnyException();
      assertThatCode(() -> ConstraintValidator.validateColumnExistence(
          List.of(), null, "test"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateColumnUniqueness 메서드는")
  class ValidateColumnUniqueness {

    @Test
    @DisplayName("컬럼이 중복되면 예외가 발생한다")
    void throwsWhenColumnDuplicate() {
      List<String> columnIds = List.of("col1", "col1");

      assertThatThrownBy(() -> ConstraintValidator.validateColumnUniqueness(
          columnIds, "test_constraint"))
          .isInstanceOf(ConstraintColumnDuplicateException.class);
    }

    @Test
    @DisplayName("대소문자 무시하고 중복 검사한다")
    void ignoresCaseWhenCheckingDuplicate() {
      List<String> columnIds = List.of("col1", "COL1");

      assertThatThrownBy(() -> ConstraintValidator.validateColumnUniqueness(
          columnIds, "test_constraint"))
          .isInstanceOf(ConstraintColumnDuplicateException.class);
    }

    @Test
    @DisplayName("모든 컬럼이 고유하면 통과한다")
    void passesWhenAllColumnsUnique() {
      List<String> columnIds = List.of("col1", "col2", "col3");

      assertThatCode(() -> ConstraintValidator.validateColumnUniqueness(
          columnIds, "test_constraint"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null이면 통과한다")
    void passesWithNull() {
      assertThatCode(() -> ConstraintValidator.validateColumnUniqueness(null, "test"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateDefinitionUniqueness 메서드는")
  class ValidateDefinitionUniqueness {

    @Test
    @DisplayName("동일한 정의가 이미 존재하면 예외가 발생한다")
    void throwsWhenDefinitionDuplicate() {
      Constraint existing = ConstraintFixture.primaryKeyConstraint();
      List<Constraint> constraints = List.of(existing);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          existing.id(),
          List.of(ConstraintFixture.constraintColumnWithColumnId("col1")));

      assertThatThrownBy(() -> ConstraintValidator.validateDefinitionUniqueness(
          constraints,
          constraintColumns,
          ConstraintKind.PRIMARY_KEY,
          null,
          null,
          List.of("col1"),
          "new_pk",
          null))
          .isInstanceOf(ConstraintDefinitionDuplicateException.class);
    }

    @Test
    @DisplayName("ignoreConstraintId와 동일한 ID는 검사에서 제외한다")
    void skipsIgnoredConstraintId() {
      Constraint existing = ConstraintFixture.primaryKeyConstraint();
      List<Constraint> constraints = List.of(existing);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          existing.id(),
          List.of(ConstraintFixture.constraintColumnWithColumnId("col1")));

      assertThatCode(() -> ConstraintValidator.validateDefinitionUniqueness(
          constraints,
          constraintColumns,
          ConstraintKind.PRIMARY_KEY,
          null,
          null,
          List.of("col1"),
          "new_pk",
          existing.id()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 정의면 통과한다")
    void passesWithDifferentDefinition() {
      Constraint existing = ConstraintFixture.primaryKeyConstraint();
      List<Constraint> constraints = List.of(existing);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          existing.id(),
          List.of(ConstraintFixture.constraintColumnWithColumnId("col1")));

      assertThatCode(() -> ConstraintValidator.validateDefinitionUniqueness(
          constraints,
          constraintColumns,
          ConstraintKind.PRIMARY_KEY,
          null,
          null,
          List.of("col2"),
          "new_pk",
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("constraints가 null이면 통과한다")
    void passesWhenConstraintsNull() {
      assertThatCode(() -> ConstraintValidator.validateDefinitionUniqueness(
          null,
          Map.of(),
          ConstraintKind.PRIMARY_KEY,
          null,
          null,
          List.of("col1"),
          "new_pk",
          null))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateUniqueSameAsPrimaryKey 메서드는")
  class ValidateUniqueSameAsPrimaryKey {

    @Test
    @DisplayName("UNIQUE가 PK와 동일한 컬럼 구성이면 예외가 발생한다")
    void throwsWhenUniqueSameAsPrimaryKey() {
      Constraint pk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(pk);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          pk.id(),
          List.of(ConstraintFixture.constraintColumn("cc1", "pk1", "col1", 0)));

      assertThatThrownBy(() -> ConstraintValidator.validateUniqueSameAsPrimaryKey(
          constraints,
          constraintColumns,
          ConstraintKind.UNIQUE,
          List.of("col1"),
          "uq_test",
          null))
          .isInstanceOf(UniqueSameAsPrimaryKeyException.class);
    }

    @Test
    @DisplayName("UNIQUE가 아닌 다른 종류면 검사하지 않는다")
    void skipsWhenNotUnique() {
      Constraint pk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(pk);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          pk.id(),
          List.of(ConstraintFixture.constraintColumn("cc1", "pk1", "col1", 0)));

      assertThatCode(() -> ConstraintValidator.validateUniqueSameAsPrimaryKey(
          constraints,
          constraintColumns,
          ConstraintKind.CHECK,
          List.of("col1"),
          "ck_test",
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 컬럼 구성이면 통과한다")
    void passesWithDifferentColumns() {
      Constraint pk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(pk);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          pk.id(),
          List.of(ConstraintFixture.constraintColumn("cc1", "pk1", "col1", 0)));

      assertThatCode(() -> ConstraintValidator.validateUniqueSameAsPrimaryKey(
          constraints,
          constraintColumns,
          ConstraintKind.UNIQUE,
          List.of("col2"),
          "uq_test",
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignoreConstraintId와 동일한 PK는 검사에서 제외한다")
    void skipsIgnoredPrimaryKey() {
      Constraint pk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(pk);
      Map<String, List<ConstraintColumn>> constraintColumns = Map.of(
          pk.id(),
          List.of(ConstraintFixture.constraintColumn("cc1", "pk1", "col1", 0)));

      assertThatCode(() -> ConstraintValidator.validateUniqueSameAsPrimaryKey(
          constraints,
          constraintColumns,
          ConstraintKind.UNIQUE,
          List.of("col1"),
          "uq_test",
          pk.id()))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateExpressionRequired 메서드는")
  class ValidateExpressionRequired {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t" })
    @DisplayName("CHECK인데 checkExpr이 null이거나 빈 문자열이면 예외가 발생한다")
    void throwsWhenCheckExprMissing(String checkExpr) {
      assertThatThrownBy(() -> ConstraintValidator.validateExpressionRequired(
          ConstraintKind.CHECK, checkExpr, null))
          .isInstanceOf(ConstraintExpressionRequiredException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t" })
    @DisplayName("DEFAULT인데 defaultExpr이 null이거나 빈 문자열이면 예외가 발생한다")
    void throwsWhenDefaultExprMissing(String defaultExpr) {
      assertThatThrownBy(() -> ConstraintValidator.validateExpressionRequired(
          ConstraintKind.DEFAULT, null, defaultExpr))
          .isInstanceOf(ConstraintExpressionRequiredException.class);
    }

    @Test
    @DisplayName("CHECK에 유효한 checkExpr이면 통과한다")
    void passesWithValidCheckExpr() {
      assertThatCode(() -> ConstraintValidator.validateExpressionRequired(
          ConstraintKind.CHECK, "value > 0", null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DEFAULT에 유효한 defaultExpr이면 통과한다")
    void passesWithValidDefaultExpr() {
      assertThatCode(() -> ConstraintValidator.validateExpressionRequired(
          ConstraintKind.DEFAULT, null, "0"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PRIMARY_KEY는 표현식 없이도 통과한다")
    void passesWithPrimaryKeyWithoutExpr() {
      assertThatCode(() -> ConstraintValidator.validateExpressionRequired(
          ConstraintKind.PRIMARY_KEY, null, null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UNIQUE는 표현식 없이도 통과한다")
    void passesWithUniqueWithoutExpr() {
      assertThatCode(() -> ConstraintValidator.validateExpressionRequired(
          ConstraintKind.UNIQUE, null, null))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validatePrimaryKeySingle 메서드는")
  class ValidatePrimaryKeySingle {

    @Test
    @DisplayName("이미 PK가 존재하면 예외가 발생한다")
    void throwsWhenPrimaryKeyExists() {
      Constraint existingPk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(existingPk);

      assertThatThrownBy(() -> ConstraintValidator.validatePrimaryKeySingle(
          constraints,
          ConstraintKind.PRIMARY_KEY,
          null))
          .isInstanceOf(MultiplePrimaryKeyConstraintException.class);
    }

    @Test
    @DisplayName("PK가 아닌 다른 종류면 검사하지 않는다")
    void skipsWhenNotPrimaryKey() {
      Constraint existingPk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(existingPk);

      assertThatCode(() -> ConstraintValidator.validatePrimaryKeySingle(
          constraints,
          ConstraintKind.UNIQUE,
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("기존 PK가 없으면 통과한다")
    void passesWhenNoPrimaryKey() {
      Constraint unique = ConstraintFixture.uniqueConstraint();
      List<Constraint> constraints = List.of(unique);

      assertThatCode(() -> ConstraintValidator.validatePrimaryKeySingle(
          constraints,
          ConstraintKind.PRIMARY_KEY,
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignoreConstraintId와 동일한 PK는 검사에서 제외한다")
    void skipsIgnoredPrimaryKey() {
      Constraint existingPk = ConstraintFixture.primaryKeyConstraintWithId("pk1");
      List<Constraint> constraints = List.of(existingPk);

      assertThatCode(() -> ConstraintValidator.validatePrimaryKeySingle(
          constraints,
          ConstraintKind.PRIMARY_KEY,
          existingPk.id()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("constraints가 null이면 통과한다")
    void passesWhenConstraintsNull() {
      assertThatCode(() -> ConstraintValidator.validatePrimaryKeySingle(
          null,
          ConstraintKind.PRIMARY_KEY,
          null))
          .doesNotThrowAnyException();
    }

  }

}
