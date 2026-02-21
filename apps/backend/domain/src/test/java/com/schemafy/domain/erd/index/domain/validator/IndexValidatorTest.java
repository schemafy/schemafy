package com.schemafy.domain.erd.index.domain.validator;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IndexValidator")
class IndexValidatorTest {

  @Nested
  @DisplayName("validateName 메서드는")
  class ValidateName {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null이거나 빈 문자열이면 예외가 발생한다")
    void throwsWhenNullOrEmpty(String name) {
      assertThatThrownBy(() -> IndexValidator.validateName(name))
          .isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "   ", "\t", "\n" })
    @DisplayName("공백 문자열이면 예외가 발생한다")
    void throwsWhenBlank(String name) {
      assertThatThrownBy(() -> IndexValidator.validateName(name))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("256자 이상이면 예외가 발생한다")
    void throwsWhenTooLong() {
      String longName = "a".repeat(256);

      assertThatThrownBy(() -> IndexValidator.validateName(longName))
          .isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "idx_column", "IDX_users_email" })
    @DisplayName("유효한 이름이면 통과한다")
    void passesWithValidName(String name) {
      assertThatCode(() -> IndexValidator.validateName(name))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("255자 이름은 통과한다")
    void passesWithMaxLengthName() {
      String maxLengthName = "a".repeat(255);

      assertThatCode(() -> IndexValidator.validateName(maxLengthName))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateType 메서드는")
  class ValidateType {

    @Test
    @DisplayName("null이면 예외가 발생한다")
    void throwsWhenNull() {
      assertThatThrownBy(() -> IndexValidator.validateType(null))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("유효한 타입이면 통과한다")
    void passesWithValidType() {
      assertThatCode(() -> IndexValidator.validateType(IndexType.BTREE))
          .doesNotThrowAnyException();
      assertThatCode(() -> IndexValidator.validateType(IndexType.HASH))
          .doesNotThrowAnyException();
      assertThatCode(() -> IndexValidator.validateType(IndexType.FULLTEXT))
          .doesNotThrowAnyException();
      assertThatCode(() -> IndexValidator.validateType(IndexType.SPATIAL))
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

      assertThatThrownBy(() -> IndexValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("seqNo가 중복되면 예외가 발생한다")
    void throwsWhenDuplicate() {
      List<Integer> seqNos = List.of(0, 0);

      assertThatThrownBy(() -> IndexValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("seqNo가 0부터 연속되지 않으면 예외가 발생한다")
    void throwsWhenNotContiguous() {
      List<Integer> seqNos = List.of(0, 2);

      assertThatThrownBy(() -> IndexValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("seqNo가 0부터 시작하지 않으면 예외가 발생한다")
    void throwsWhenNotStartingFromZero() {
      List<Integer> seqNos = List.of(1, 2);

      assertThatThrownBy(() -> IndexValidator.validateSeqNoIntegrity(seqNos))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("유효한 seqNo 리스트면 통과한다")
    void passesWithValidSeqNos() {
      List<Integer> seqNos = List.of(0, 1, 2);

      assertThatCode(() -> IndexValidator.validateSeqNoIntegrity(seqNos))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null이나 빈 리스트면 통과한다")
    void passesWithNullOrEmpty() {
      assertThatCode(() -> IndexValidator.validateSeqNoIntegrity(null))
          .doesNotThrowAnyException();
      assertThatCode(() -> IndexValidator.validateSeqNoIntegrity(List.of()))
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
      List<IndexColumn> indexColumns = List.of(
          IndexFixture.indexColumnWithColumnId("col1"),
          IndexFixture.indexColumnWithColumnId("col2"));

      assertThatThrownBy(() -> IndexValidator.validateColumnExistence(
          tableColumns, indexColumns, "test_index"))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("col2");
    }

    @Test
    @DisplayName("모든 컬럼이 존재하면 통과한다")
    void passesWhenAllColumnsExist() {
      List<Column> tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));
      List<IndexColumn> indexColumns = List.of(
          IndexFixture.indexColumnWithColumnId("col1"),
          IndexFixture.indexColumnWithColumnId("col2"));

      assertThatCode(() -> IndexValidator.validateColumnExistence(
          tableColumns, indexColumns, "test_index"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("대소문자 무시하고 비교한다")
    void ignoresCaseWhenComparing() {
      List<Column> tableColumns = List.of(ColumnFixture.columnWithId("COL1"));
      List<IndexColumn> indexColumns = List.of(IndexFixture.indexColumnWithColumnId("col1"));

      assertThatCode(() -> IndexValidator.validateColumnExistence(
          tableColumns, indexColumns, "test_index"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("indexColumns가 null이면 통과한다")
    void passesWhenIndexColumnsNull() {
      List<Column> tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      assertThatCode(() -> IndexValidator.validateColumnExistence(
          tableColumns, null, "test_index"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateColumnUniqueness 메서드는")
  class ValidateColumnUniqueness {

    @Test
    @DisplayName("컬럼이 중복되면 예외가 발생한다")
    void throwsWhenColumnDuplicate() {
      List<IndexColumn> indexColumns = List.of(
          IndexFixture.indexColumnWithColumnId("col1"),
          IndexFixture.indexColumnWithColumnId("col1"));

      assertThatThrownBy(() -> IndexValidator.validateColumnUniqueness(
          indexColumns, "test_index"))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("대소문자 무시하고 중복 검사한다")
    void ignoresCaseWhenCheckingDuplicate() {
      List<IndexColumn> indexColumns = List.of(
          IndexFixture.indexColumnWithColumnId("col1"),
          IndexFixture.indexColumnWithColumnId("COL1"));

      assertThatThrownBy(() -> IndexValidator.validateColumnUniqueness(
          indexColumns, "test_index"))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("모든 컬럼이 고유하면 통과한다")
    void passesWhenAllColumnsUnique() {
      List<IndexColumn> indexColumns = List.of(
          IndexFixture.indexColumnWithColumnId("col1"),
          IndexFixture.indexColumnWithColumnId("col2"));

      assertThatCode(() -> IndexValidator.validateColumnUniqueness(
          indexColumns, "test_index"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null이면 통과한다")
    void passesWithNull() {
      assertThatCode(() -> IndexValidator.validateColumnUniqueness(null, "test"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateSortDirections 메서드는")
  class ValidateSortDirections {

    @Test
    @DisplayName("sortDirection이 null이면 예외가 발생한다")
    void throwsWhenSortDirectionNull() {
      IndexColumn columnWithNullDirection = new IndexColumn(
          "ic1", "idx1", "col1", 0, null);
      List<IndexColumn> indexColumns = List.of(columnWithNullDirection);

      assertThatThrownBy(() -> IndexValidator.validateSortDirections(
          indexColumns, "test_index"))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("모든 컬럼의 sortDirection이 유효하면 통과한다")
    void passesWhenAllSortDirectionsValid() {
      List<IndexColumn> indexColumns = List.of(
          IndexFixture.indexColumnWithSortDirection(SortDirection.ASC),
          IndexFixture.indexColumnWithSortDirection(SortDirection.DESC));

      assertThatCode(() -> IndexValidator.validateSortDirections(
          indexColumns, "test_index"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null이면 통과한다")
    void passesWithNull() {
      assertThatCode(() -> IndexValidator.validateSortDirections(null, "test"))
          .doesNotThrowAnyException();
    }

  }

  @Nested
  @DisplayName("validateDefinitionUniqueness 메서드는")
  class ValidateDefinitionUniqueness {

    @Test
    @DisplayName("동일한 정의가 이미 존재하면 예외가 발생한다")
    void throwsWhenDefinitionDuplicate() {
      Index existing = IndexFixture.btreeIndexWithId("idx1");
      List<Index> indexes = List.of(existing);
      IndexColumn existingColumn = IndexFixture.indexColumn(
          "ic1", "idx1", "col1", 0, SortDirection.ASC);
      Map<String, List<IndexColumn>> indexColumns = Map.of(
          existing.id(),
          List.of(existingColumn));
      List<IndexColumn> candidateColumns = List.of(
          IndexFixture.indexColumn("ic2", "idx2", "col1", 0, SortDirection.ASC));

      assertThatThrownBy(() -> IndexValidator.validateDefinitionUniqueness(
          indexes,
          indexColumns,
          IndexType.BTREE,
          candidateColumns,
          "new_idx",
          null))
          .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("ignoreIndexId와 동일한 ID는 검사에서 제외한다")
    void skipsIgnoredIndexId() {
      Index existing = IndexFixture.btreeIndexWithId("idx1");
      List<Index> indexes = List.of(existing);
      IndexColumn existingColumn = IndexFixture.indexColumn(
          "ic1", "idx1", "col1", 0, SortDirection.ASC);
      Map<String, List<IndexColumn>> indexColumns = Map.of(
          existing.id(),
          List.of(existingColumn));
      List<IndexColumn> candidateColumns = List.of(
          IndexFixture.indexColumn("ic2", "idx2", "col1", 0, SortDirection.ASC));

      assertThatCode(() -> IndexValidator.validateDefinitionUniqueness(
          indexes,
          indexColumns,
          IndexType.BTREE,
          candidateColumns,
          "new_idx",
          existing.id()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 컬럼 구성이면 통과한다")
    void passesWithDifferentColumns() {
      Index existing = IndexFixture.btreeIndexWithId("idx1");
      List<Index> indexes = List.of(existing);
      IndexColumn existingColumn = IndexFixture.indexColumn(
          "ic1", "idx1", "col1", 0, SortDirection.ASC);
      Map<String, List<IndexColumn>> indexColumns = Map.of(
          existing.id(),
          List.of(existingColumn));
      List<IndexColumn> candidateColumns = List.of(
          IndexFixture.indexColumn("ic2", "idx2", "col2", 0, SortDirection.ASC));

      assertThatCode(() -> IndexValidator.validateDefinitionUniqueness(
          indexes,
          indexColumns,
          IndexType.BTREE,
          candidateColumns,
          "new_idx",
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 인덱스 타입이면 통과한다")
    void passesWithDifferentType() {
      Index existing = IndexFixture.btreeIndexWithId("idx1");
      List<Index> indexes = List.of(existing);
      IndexColumn existingColumn = IndexFixture.indexColumn(
          "ic1", "idx1", "col1", 0, SortDirection.ASC);
      Map<String, List<IndexColumn>> indexColumns = Map.of(
          existing.id(),
          List.of(existingColumn));
      List<IndexColumn> candidateColumns = List.of(
          IndexFixture.indexColumn("ic2", "idx2", "col1", 0, SortDirection.ASC));

      assertThatCode(() -> IndexValidator.validateDefinitionUniqueness(
          indexes,
          indexColumns,
          IndexType.HASH,
          candidateColumns,
          "new_idx",
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 sortDirection이면 통과한다")
    void passesWithDifferentSortDirection() {
      Index existing = IndexFixture.btreeIndexWithId("idx1");
      List<Index> indexes = List.of(existing);
      IndexColumn existingColumn = IndexFixture.indexColumn(
          "ic1", "idx1", "col1", 0, SortDirection.ASC);
      Map<String, List<IndexColumn>> indexColumns = Map.of(
          existing.id(),
          List.of(existingColumn));
      List<IndexColumn> candidateColumns = List.of(
          IndexFixture.indexColumn("ic2", "idx2", "col1", 0, SortDirection.DESC));

      assertThatCode(() -> IndexValidator.validateDefinitionUniqueness(
          indexes,
          indexColumns,
          IndexType.BTREE,
          candidateColumns,
          "new_idx",
          null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("indexes가 null이면 통과한다")
    void passesWhenIndexesNull() {
      List<IndexColumn> candidateColumns = List.of(
          IndexFixture.indexColumn("ic1", "idx1", "col1", 0, SortDirection.ASC));

      assertThatCode(() -> IndexValidator.validateDefinitionUniqueness(
          null,
          Map.of(),
          IndexType.BTREE,
          candidateColumns,
          "new_idx",
          null))
          .doesNotThrowAnyException();
    }

  }

}
