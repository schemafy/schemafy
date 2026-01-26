package com.schemafy.domain.erd.relationship.domain.validator;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipEmptyException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameInvalidException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipPositionInvalidException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RelationshipValidator")
class RelationshipValidatorTest {

  @Nested
  @DisplayName("validateName 메서드는")
  class ValidateName {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null이거나 빈 문자열이면 예외가 발생한다")
    void throwsWhenNullOrEmpty(String name) {
      assertThatThrownBy(() -> RelationshipValidator.validateName(name))
          .isInstanceOf(RelationshipNameInvalidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "   ", "\t", "\n" })
    @DisplayName("공백 문자열이면 예외가 발생한다")
    void throwsWhenBlank(String name) {
      assertThatThrownBy(() -> RelationshipValidator.validateName(name))
          .isInstanceOf(RelationshipNameInvalidException.class);
    }

    @Test
    @DisplayName("256자 이상이면 예외가 발생한다")
    void throwsWhenTooLong() {
      String longName = "a".repeat(256);

      assertThatThrownBy(() -> RelationshipValidator.validateName(longName))
          .isInstanceOf(RelationshipNameInvalidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "fk_relationship", "FK_users_orders" })
    @DisplayName("유효한 이름이면 통과한다")
    void passesWithValidName(String name) {
      assertThatCode(() -> RelationshipValidator.validateName(name))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("255자 이름은 통과한다")
    void passesWithMaxLengthName() {
      String maxLengthName = "a".repeat(255);

      assertThatCode(() -> RelationshipValidator.validateName(maxLengthName))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateColumnsNotEmpty 메서드는")
  class ValidateColumnsNotEmpty {

    @Test
    @DisplayName("컬럼 리스트가 null이면 예외가 발생한다")
    void throwsWhenNull() {
      assertThatThrownBy(() -> RelationshipValidator.validateColumnsNotEmpty(null, "test"))
          .isInstanceOf(RelationshipEmptyException.class);
    }

    @Test
    @DisplayName("컬럼 리스트가 비어있으면 예외가 발생한다")
    void throwsWhenEmpty() {
      assertThatThrownBy(() -> RelationshipValidator.validateColumnsNotEmpty(List.of(), "test"))
          .isInstanceOf(RelationshipEmptyException.class);
    }

    @Test
    @DisplayName("컬럼이 있으면 통과한다")
    void passesWithColumns() {
      List<RelationshipColumn> columns = List.of(RelationshipFixture.defaultRelationshipColumn());

      assertThatCode(() -> RelationshipValidator.validateColumnsNotEmpty(columns, "test"))
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

      assertThatThrownBy(() -> RelationshipValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(RelationshipPositionInvalidException.class);
    }

    @Test
    @DisplayName("seqNo가 중복되면 예외가 발생한다")
    void throwsWhenDuplicate() {
      List<Integer> seqNos = List.of(0, 0);

      assertThatThrownBy(() -> RelationshipValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(RelationshipPositionInvalidException.class);
    }

    @Test
    @DisplayName("seqNo가 0부터 연속되지 않으면 예외가 발생한다")
    void throwsWhenNotContiguous() {
      List<Integer> seqNos = List.of(0, 2);

      assertThatThrownBy(() -> RelationshipValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(RelationshipPositionInvalidException.class);
    }

    @Test
    @DisplayName("seqNo가 0부터 시작하지 않으면 예외가 발생한다")
    void throwsWhenNotStartingFromZero() {
      List<Integer> seqNos = List.of(1, 2);

      assertThatThrownBy(() -> RelationshipValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(RelationshipPositionInvalidException.class);
    }

    @Test
    @DisplayName("유효한 seqNo 리스트면 통과한다")
    void passesWithValidSeqNos() {
      List<Integer> seqNos = List.of(0, 1, 2);

      assertThatCode(() -> RelationshipValidator.validateSeqNoIntegrity(seqNos))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null이나 빈 리스트면 통과한다")
    void passesWithNullOrEmpty() {
      assertThatCode(() -> RelationshipValidator.validateSeqNoIntegrity(null))
          .doesNotThrowAnyException();
      assertThatCode(() -> RelationshipValidator.validateSeqNoIntegrity(List.of()))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateColumnExistence 메서드는")
  class ValidateColumnExistence {

    @Test
    @DisplayName("FK 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenFkColumnNotExists() {
      List<Column> fkColumns = List.of();
      List<Column> pkColumns = List.of(ColumnFixture.columnWithId("pk1"));
      List<RelationshipColumn> relationshipColumns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk1", "fk1", 0));

      assertThatThrownBy(() -> RelationshipValidator.validateColumnExistence(
          fkColumns, pkColumns, relationshipColumns, "test"))
          .isInstanceOf(RelationshipColumnNotExistException.class)
          .hasMessageContaining("FK column");
    }

    @Test
    @DisplayName("PK 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenPkColumnNotExists() {
      List<Column> fkColumns = List.of(ColumnFixture.columnWithId("fk1"));
      List<Column> pkColumns = List.of();
      List<RelationshipColumn> relationshipColumns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk1", "fk1", 0));

      assertThatThrownBy(() -> RelationshipValidator.validateColumnExistence(
          fkColumns, pkColumns, relationshipColumns, "test"))
          .isInstanceOf(RelationshipColumnNotExistException.class)
          .hasMessageContaining("PK column");
    }

    @Test
    @DisplayName("모든 컬럼이 존재하면 통과한다")
    void passesWhenAllColumnsExist() {
      List<Column> fkColumns = List.of(ColumnFixture.columnWithId("fk1"));
      List<Column> pkColumns = List.of(ColumnFixture.columnWithId("pk1"));
      List<RelationshipColumn> relationshipColumns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk1", "fk1", 0));

      assertThatCode(() -> RelationshipValidator.validateColumnExistence(
          fkColumns, pkColumns, relationshipColumns, "test"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateColumnUniqueness 메서드는")
  class ValidateColumnUniqueness {

    @Test
    @DisplayName("FK 컬럼이 중복되면 예외가 발생한다")
    void throwsWhenFkColumnDuplicate() {
      List<RelationshipColumn> columns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk1", "fk1", 0),
          RelationshipFixture.relationshipColumn("rc2", "rel1", "pk2", "fk1", 1));

      assertThatThrownBy(() -> RelationshipValidator.validateColumnUniqueness(columns, "test"))
          .isInstanceOf(RelationshipColumnDuplicateException.class)
          .hasMessageContaining("FK column");
    }

    @Test
    @DisplayName("PK 컬럼이 중복되면 예외가 발생한다")
    void throwsWhenPkColumnDuplicate() {
      List<RelationshipColumn> columns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk1", "fk1", 0),
          RelationshipFixture.relationshipColumn("rc2", "rel1", "pk1", "fk2", 1));

      assertThatThrownBy(() -> RelationshipValidator.validateColumnUniqueness(columns, "test"))
          .isInstanceOf(RelationshipColumnDuplicateException.class)
          .hasMessageContaining("PK column");
    }

    @Test
    @DisplayName("모든 컬럼이 고유하면 통과한다")
    void passesWhenAllColumnsUnique() {
      List<RelationshipColumn> columns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk1", "fk1", 0),
          RelationshipFixture.relationshipColumn("rc2", "rel1", "pk2", "fk2", 1));

      assertThatCode(() -> RelationshipValidator.validateColumnUniqueness(columns, "test"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateIdentifyingCycle 메서드는")
  class ValidateIdentifyingCycle {

    @Nested
    @DisplayName("IDENTIFYING 관계에서")
    class WithIdentifyingRelationship {

      @Test
      @DisplayName("직접 순환 (A -> B -> A) 발생 시 예외가 발생한다")
      void throwsWhenDirectCycle() {
        Relationship existing = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship newRel = new Relationship(
            "rel2", "tableB", "tableA", "fk_b_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        assertThatThrownBy(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(existing), null, newRel))
            .isInstanceOf(RelationshipCyclicReferenceException.class);
      }

      @Test
      @DisplayName("간접 순환 (A -> B -> C -> A) 발생 시 예외가 발생한다")
      void throwsWhenIndirectCycle() {
        Relationship ab = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship bc = new Relationship(
            "rel2", "tableB", "tableC", "fk_b_c",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship ca = new Relationship(
            "rel3", "tableC", "tableA", "fk_c_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        assertThatThrownBy(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(ab, bc), null, ca))
            .isInstanceOf(RelationshipCyclicReferenceException.class);
      }

      @Test
      @DisplayName("자기 자신 참조 시 예외가 발생한다")
      void throwsWhenSelfReference() {
        Relationship selfRef = new Relationship(
            "rel1", "tableA", "tableA", "fk_self",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        assertThatThrownBy(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(), null, selfRef))
            .isInstanceOf(RelationshipCyclicReferenceException.class);
      }

      @Test
      @DisplayName("순환이 없으면 통과한다")
      void passesWithoutCycle() {
        Relationship ab = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship bc = new Relationship(
            "rel2", "tableB", "tableC", "fk_b_c",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        assertThatCode(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(ab), null, bc))
            .doesNotThrowAnyException();
      }

    }

    @Nested
    @DisplayName("NON_IDENTIFYING 관계에서")
    class WithNonIdentifyingRelationship {

      @Test
      @DisplayName("순환이 있어도 통과한다")
      void passesWithCycle() {
        Relationship existing = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship newRel = new Relationship(
            "rel2", "tableB", "tableA", "fk_b_a",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        assertThatCode(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(existing), null, newRel))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName("NON_IDENTIFYING은 순환 검사에 포함되지 않는다")
      void nonIdentifyingNotIncludedInCycleCheck() {
        Relationship nonIdentifying = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship identifying = new Relationship(
            "rel2", "tableB", "tableA", "fk_b_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        assertThatCode(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(nonIdentifying), null, identifying))
            .doesNotThrowAnyException();
      }

    }

    @Nested
    @DisplayName("RelationshipKindChange를 사용할 때")
    class WithRelationshipKindChange {

      @Test
      @DisplayName("NON_IDENTIFYING에서 IDENTIFYING으로 변경 시 순환이 생기면 예외가 발생한다")
      void throwsWhenChangingToIdentifyingCreatesCycle() {
        Relationship ab = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship ba = new Relationship(
            "rel2", "tableB", "tableA", "fk_b_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        RelationshipValidator.RelationshipKindChange change =
            new RelationshipValidator.RelationshipKindChange("rel1", RelationshipKind.IDENTIFYING);

        assertThatThrownBy(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(ab, ba), change, null))
            .isInstanceOf(RelationshipCyclicReferenceException.class);
      }

      @Test
      @DisplayName("IDENTIFYING에서 NON_IDENTIFYING으로 변경 시 순환이 해제되면 통과한다")
      void passesWhenChangingToNonIdentifyingBreaksCycle() {
        Relationship ab = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship ba = new Relationship(
            "rel2", "tableB", "tableA", "fk_b_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        RelationshipValidator.RelationshipKindChange change =
            new RelationshipValidator.RelationshipKindChange(
                "rel1", RelationshipKind.NON_IDENTIFYING);

        assertThatCode(() -> RelationshipValidator.validateIdentifyingCycle(
            List.of(ab, ba), change, null))
            .doesNotThrowAnyException();
      }

    }

    @Nested
    @DisplayName("detectIdentifyingCycle 메서드는")
    class DetectIdentifyingCycle {

      @Test
      @DisplayName("순환이 있으면 IdentifyingCycle을 반환한다")
      void returnsCycleWhenExists() {
        Relationship ab = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship ba = new Relationship(
            "rel2", "tableB", "tableA", "fk_b_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        RelationshipValidator.IdentifyingCycle cycle =
            RelationshipValidator.detectIdentifyingCycle(List.of(ab, ba), null, null);

        org.assertj.core.api.Assertions.assertThat(cycle).isNotNull();
      }

      @Test
      @DisplayName("순환이 없으면 null을 반환한다")
      void returnsNullWhenNoCycle() {
        Relationship ab = new Relationship(
            "rel1", "tableA", "tableB", "fk_a_b",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        Relationship bc = new Relationship(
            "rel2", "tableB", "tableC", "fk_b_c",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

        RelationshipValidator.IdentifyingCycle cycle =
            RelationshipValidator.detectIdentifyingCycle(List.of(ab, bc), null, null);

        org.assertj.core.api.Assertions.assertThat(cycle).isNull();
      }

      @Test
      @DisplayName("빈 리스트면 null을 반환한다")
      void returnsNullWhenEmpty() {
        RelationshipValidator.IdentifyingCycle cycle =
            RelationshipValidator.detectIdentifyingCycle(List.of(), null, null);

        org.assertj.core.api.Assertions.assertThat(cycle).isNull();
      }

    }

  }

}
