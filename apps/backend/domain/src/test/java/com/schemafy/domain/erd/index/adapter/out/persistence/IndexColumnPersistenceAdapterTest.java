package com.schemafy.domain.erd.index.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
    IndexPersistenceAdapter.class,
    IndexColumnPersistenceAdapter.class,
    IndexMapper.class,
    IndexColumnMapper.class,
    R2dbcTestConfiguration.class
})
@DisplayName("IndexColumnPersistenceAdapter")
class IndexColumnPersistenceAdapterTest {

  private static final String INDEX_ID_1 = IndexFixture.DEFAULT_ID;
  private static final String INDEX_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5IX2";
  private static final String COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5IC1";
  private static final String COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5IC2";
  private static final String COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5IC3";
  private static final String TABLE_COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5TC1";
  private static final String TABLE_COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5TC2";
  private static final String TABLE_COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5TC3";

  @Autowired
  IndexColumnPersistenceAdapter sut;

  @Autowired
  IndexPersistenceAdapter indexAdapter;

  @Autowired
  IndexRepository indexRepository;

  @Autowired
  IndexColumnRepository indexColumnRepository;

  @Autowired
  DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    indexColumnRepository.deleteAll().block();
    indexRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("createIndexColumn 메서드는")
  class CreateIndexColumn {

    @BeforeEach
    void setUpIndex() {
      var index = IndexFixture.defaultIndex();
      indexAdapter.createIndex(index).block();
    }

    @Test
    @DisplayName("인덱스 컬럼을 저장하고 반환한다")
    void savesAndReturnsIndexColumn() {
      var column = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);

      StepVerifier.create(sut.createIndexColumn(column))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(COLUMN_ID_1);
            assertThat(saved.indexId()).isEqualTo(INDEX_ID_1);
            assertThat(saved.columnId()).isEqualTo(TABLE_COLUMN_ID_1);
            assertThat(saved.seqNo()).isEqualTo(0);
            assertThat(saved.sortDirection()).isEqualTo(SortDirection.ASC);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("seqNo가 다른 여러 인덱스 컬럼을 저장한다")
    void savesMultipleColumnsWithDifferentSeqNo() {
      var column1 = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      var column2 = IndexFixture.indexColumn(
          COLUMN_ID_2, INDEX_ID_1, TABLE_COLUMN_ID_2, 1, SortDirection.DESC);

      sut.createIndexColumn(column1).block();
      sut.createIndexColumn(column2).block();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(2);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(0).sortDirection()).isEqualTo(SortDirection.ASC);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
            assertThat(columns.get(1).sortDirection()).isEqualTo(SortDirection.DESC);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findIndexColumnById 메서드는")
  class FindIndexColumnById {

    @BeforeEach
    void setUpIndex() {
      var index = IndexFixture.defaultIndex();
      indexAdapter.createIndex(index).block();
    }

    @Test
    @DisplayName("존재하는 인덱스 컬럼을 반환한다")
    void returnsExistingIndexColumn() {
      var column = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      sut.createIndexColumn(column).block();

      StepVerifier.create(sut.findIndexColumnById(COLUMN_ID_1))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(COLUMN_ID_1);
            assertThat(found.indexId()).isEqualTo(INDEX_ID_1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findIndexColumnById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findIndexColumnsByIndexId 메서드는")
  class FindIndexColumnsByIndexId {

    @BeforeEach
    void setUpIndexes() {
      var index1 = IndexFixture.indexWithId(INDEX_ID_1);
      var index2 = IndexFixture.indexWithIdAndName(INDEX_ID_2, "idx_other");
      indexAdapter.createIndex(index1).block();
      indexAdapter.createIndex(index2).block();
    }

    @Test
    @DisplayName("해당 인덱스의 컬럼들을 seqNo 순으로 반환한다")
    void returnsColumnsOrderedBySeqNo() {
      var column1 = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 2, SortDirection.ASC);
      var column2 = IndexFixture.indexColumn(
          COLUMN_ID_2, INDEX_ID_1, TABLE_COLUMN_ID_2, 0, SortDirection.ASC);
      var column3 = IndexFixture.indexColumn(
          COLUMN_ID_3, INDEX_ID_1, TABLE_COLUMN_ID_3, 1, SortDirection.DESC);
      sut.createIndexColumn(column1).block();
      sut.createIndexColumn(column2).block();
      sut.createIndexColumn(column3).block();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_2); // seqNo 0
            assertThat(columns.get(1).id()).isEqualTo(COLUMN_ID_3); // seqNo 1
            assertThat(columns.get(2).id()).isEqualTo(COLUMN_ID_1); // seqNo 2
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 인덱스의 컬럼은 반환하지 않는다")
    void returnsOnlyColumnsOfSpecifiedIndex() {
      var column1 = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      var column2 = IndexFixture.indexColumn(
          COLUMN_ID_2, INDEX_ID_2, TABLE_COLUMN_ID_2, 0, SortDirection.ASC);
      sut.createIndexColumn(column1).block();
      sut.createIndexColumn(column2).block();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("인덱스에 컬럼이 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoColumns() {
      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeIndexColumnPositions 메서드는")
  class ChangeIndexColumnPositions {

    @BeforeEach
    void setUpIndexWithColumns() {
      var index = IndexFixture.defaultIndex();
      indexAdapter.createIndex(index).block();

      var column1 = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      var column2 = IndexFixture.indexColumn(
          COLUMN_ID_2, INDEX_ID_1, TABLE_COLUMN_ID_2, 1, SortDirection.ASC);
      var column3 = IndexFixture.indexColumn(
          COLUMN_ID_3, INDEX_ID_1, TABLE_COLUMN_ID_3, 2, SortDirection.ASC);
      sut.createIndexColumn(column1).block();
      sut.createIndexColumn(column2).block();
      sut.createIndexColumn(column3).block();
    }

    @Test
    @DisplayName("컬럼 위치를 변경한다")
    void changesColumnPositions() {
      var reorderedColumns = List.of(
          new IndexColumn(COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 2, SortDirection.ASC),
          new IndexColumn(COLUMN_ID_2, INDEX_ID_1, TABLE_COLUMN_ID_2, 0, SortDirection.ASC),
          new IndexColumn(COLUMN_ID_3, INDEX_ID_1, TABLE_COLUMN_ID_3, 1, SortDirection.ASC)
      );

      StepVerifier.create(sut.changeIndexColumnPositions(INDEX_ID_1, reorderedColumns))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_2); // new seqNo 0
            assertThat(columns.get(1).id()).isEqualTo(COLUMN_ID_3); // new seqNo 1
            assertThat(columns.get(2).id()).isEqualTo(COLUMN_ID_1); // new seqNo 2
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("빈 리스트로 호출하면 아무것도 변경하지 않는다")
    void doesNothingWithEmptyList() {
      StepVerifier.create(sut.changeIndexColumnPositions(INDEX_ID_1, List.of()))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
            assertThat(columns.get(2).seqNo()).isEqualTo(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("null로 호출하면 아무것도 변경하지 않는다")
    void doesNothingWithNull() {
      StepVerifier.create(sut.changeIndexColumnPositions(INDEX_ID_1, null))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeIndexColumnSortDirection 메서드는")
  class ChangeIndexColumnSortDirection {

    @BeforeEach
    void setUpIndexWithColumn() {
      var index = IndexFixture.defaultIndex();
      indexAdapter.createIndex(index).block();

      var column = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      sut.createIndexColumn(column).block();
    }

    @Test
    @DisplayName("인덱스 컬럼의 정렬 방향을 변경한다")
    void changesSortDirection() {
      StepVerifier.create(sut.changeIndexColumnSortDirection(COLUMN_ID_1, SortDirection.DESC))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnById(COLUMN_ID_1))
          .assertNext(found -> assertThat(found.sortDirection()).isEqualTo(SortDirection.DESC))
          .verifyComplete();
    }

    @Test
    @DisplayName("ASC에서 DESC로 변경한다")
    void changesFromAscToDesc() {
      StepVerifier.create(sut.changeIndexColumnSortDirection(COLUMN_ID_1, SortDirection.DESC))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnById(COLUMN_ID_1))
          .assertNext(found -> assertThat(found.sortDirection()).isEqualTo(SortDirection.DESC))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteIndexColumn 메서드는")
  class DeleteIndexColumn {

    @BeforeEach
    void setUpIndexWithColumn() {
      var index = IndexFixture.defaultIndex();
      indexAdapter.createIndex(index).block();

      var column = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      sut.createIndexColumn(column).block();
    }

    @Test
    @DisplayName("인덱스 컬럼을 삭제한다")
    void deletesIndexColumn() {
      StepVerifier.create(sut.deleteIndexColumn(COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnById(COLUMN_ID_1))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 컬럼을 삭제해도 에러가 발생하지 않는다")
    void doesNotThrowWhenColumnNotExists() {
      StepVerifier.create(sut.deleteIndexColumn("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteByIndexId 메서드는")
  class DeleteByIndexId {

    @BeforeEach
    void setUpIndexesWithColumns() {
      var index1 = IndexFixture.indexWithId(INDEX_ID_1);
      var index2 = IndexFixture.indexWithIdAndName(INDEX_ID_2, "idx_other");
      indexAdapter.createIndex(index1).block();
      indexAdapter.createIndex(index2).block();

      var column1 = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      var column2 = IndexFixture.indexColumn(
          COLUMN_ID_2, INDEX_ID_1, TABLE_COLUMN_ID_2, 1, SortDirection.ASC);
      var column3 = IndexFixture.indexColumn(
          COLUMN_ID_3, INDEX_ID_2, TABLE_COLUMN_ID_3, 0, SortDirection.ASC);
      sut.createIndexColumn(column1).block();
      sut.createIndexColumn(column2).block();
      sut.createIndexColumn(column3).block();
    }

    @Test
    @DisplayName("해당 인덱스의 모든 컬럼을 삭제한다")
    void deletesAllColumnsOfIndex() {
      StepVerifier.create(sut.deleteByIndexId(INDEX_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_1))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 인덱스의 컬럼은 삭제하지 않는다")
    void doesNotDeleteColumnsOfOtherIndex() {
      StepVerifier.create(sut.deleteByIndexId(INDEX_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnsByIndexId(INDEX_ID_2))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_3);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteByColumnId 메서드는")
  class DeleteByColumnId {

    @BeforeEach
    void setUpIndexWithColumns() {
      var index = IndexFixture.defaultIndex();
      indexAdapter.createIndex(index).block();

      var column1 = IndexFixture.indexColumn(
          COLUMN_ID_1, INDEX_ID_1, TABLE_COLUMN_ID_1, 0, SortDirection.ASC);
      var column2 = IndexFixture.indexColumn(
          COLUMN_ID_2, INDEX_ID_1, TABLE_COLUMN_ID_2, 1, SortDirection.ASC);
      sut.createIndexColumn(column1).block();
      sut.createIndexColumn(column2).block();
    }

    @Test
    @DisplayName("해당 테이블 컬럼을 참조하는 인덱스 컬럼을 삭제한다")
    void deletesIndexColumnsByColumnId() {
      StepVerifier.create(sut.deleteByColumnId(TABLE_COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findIndexColumnById(COLUMN_ID_1))
          .verifyComplete();

      // 다른 테이블 컬럼을 참조하는 인덱스 컬럼은 남아있다
      StepVerifier.create(sut.findIndexColumnById(COLUMN_ID_2))
          .assertNext(found -> assertThat(found.id()).isEqualTo(COLUMN_ID_2))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 컬럼 ID로 호출해도 에러가 발생하지 않는다")
    void doesNotThrowWhenColumnIdNotExists() {
      StepVerifier.create(sut.deleteByColumnId("non-existent-column-id"))
          .verifyComplete();
    }

  }

}
