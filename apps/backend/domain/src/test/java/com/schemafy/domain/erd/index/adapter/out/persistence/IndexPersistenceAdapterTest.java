package com.schemafy.domain.erd.index.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
  IndexPersistenceAdapter.class,
  IndexColumnPersistenceAdapter.class,
  IndexMapper.class,
  IndexColumnMapper.class,
  R2dbcTestConfiguration.class
})
@DisplayName("IndexPersistenceAdapter")
class IndexPersistenceAdapterTest {

  private static final String TABLE_ID = IndexFixture.DEFAULT_TABLE_ID;
  private static final String OTHER_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTT";
  private static final String INDEX_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5IX1";
  private static final String INDEX_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5IX2";

  @Autowired
  IndexPersistenceAdapter sut;

  @Autowired
  IndexColumnPersistenceAdapter indexColumnAdapter;

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
  @DisplayName("createIndex 메서드는")
  class CreateIndex {

    @Test
    @DisplayName("인덱스를 저장하고 반환한다")
    void savesAndReturnsIndex() {
      var index = IndexFixture.defaultIndex();

      StepVerifier.create(sut.createIndex(index))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(index.id());
            assertThat(saved.tableId()).isEqualTo(index.tableId());
            assertThat(saved.name()).isEqualTo(index.name());
            assertThat(saved.type()).isEqualTo(index.type());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("BTREE 인덱스를 저장한다")
    void savesBtreeIndex() {
      var index = IndexFixture.btreeIndex();

      StepVerifier.create(sut.createIndex(index))
          .assertNext(saved -> {
            assertThat(saved.type()).isEqualTo(IndexType.BTREE);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("HASH 인덱스를 저장한다")
    void savesHashIndex() {
      var index = IndexFixture.hashIndex();

      StepVerifier.create(sut.createIndex(index))
          .assertNext(saved -> {
            assertThat(saved.type()).isEqualTo(IndexType.HASH);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("FULLTEXT 인덱스를 저장한다")
    void savesFulltextIndex() {
      var index = IndexFixture.fulltextIndex();

      StepVerifier.create(sut.createIndex(index))
          .assertNext(saved -> {
            assertThat(saved.type()).isEqualTo(IndexType.FULLTEXT);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("SPATIAL 인덱스를 저장한다")
    void savesSpatialIndex() {
      var index = IndexFixture.spatialIndex();

      StepVerifier.create(sut.createIndex(index))
          .assertNext(saved -> {
            assertThat(saved.type()).isEqualTo(IndexType.SPATIAL);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findIndexById 메서드는")
  class FindIndexById {

    @Test
    @DisplayName("존재하는 인덱스를 반환한다")
    void returnsExistingIndex() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.findIndexById(index.id()))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(index.id());
            assertThat(found.name()).isEqualTo(index.name());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findIndexById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findIndexesByTableId 메서드는")
  class FindIndexesByTableId {

    @Test
    @DisplayName("해당 테이블의 인덱스들을 반환한다")
    void returnsIndexesOfTable() {
      var index1 = IndexFixture.indexWithId(INDEX_ID_1);
      var index2 = IndexFixture.indexWithIdAndName(INDEX_ID_2, "idx_other");
      sut.createIndex(index1).block();
      sut.createIndex(index2).block();

      StepVerifier.create(sut.findIndexesByTableId(TABLE_ID))
          .assertNext(indexes -> {
            assertThat(indexes).hasSize(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 테이블의 인덱스는 반환하지 않는다")
    void returnsOnlyIndexesOfSpecifiedTable() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.findIndexesByTableId(OTHER_TABLE_ID))
          .assertNext(indexes -> assertThat(indexes).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeIndexName 메서드는")
  class ChangeIndexName {

    @Test
    @DisplayName("인덱스 이름을 변경한다")
    void changesIndexName() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();
      var newName = "new_idx_name";

      StepVerifier.create(sut.changeIndexName(index.id(), newName))
          .verifyComplete();

      StepVerifier.create(sut.findIndexById(index.id()))
          .assertNext(found -> assertThat(found.name()).isEqualTo(newName))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 인덱스면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      StepVerifier.create(sut.changeIndexName("non-existent-id", "new_name"))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NOT_FOUND))
          .verify();
    }

  }

  @Nested
  @DisplayName("changeIndexType 메서드는")
  class ChangeIndexType {

    @Test
    @DisplayName("인덱스 타입을 변경한다")
    void changesIndexType() {
      var index = IndexFixture.btreeIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.changeIndexType(index.id(), IndexType.HASH))
          .verifyComplete();

      StepVerifier.create(sut.findIndexById(index.id()))
          .assertNext(found -> assertThat(found.type()).isEqualTo(IndexType.HASH))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 인덱스면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      StepVerifier.create(sut.changeIndexType("non-existent-id", IndexType.HASH))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NOT_FOUND))
          .verify();
    }

  }

  @Nested
  @DisplayName("deleteIndex 메서드는")
  class DeleteIndex {

    @Test
    @DisplayName("인덱스를 삭제한다")
    void deletesIndex() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.deleteIndex(index.id()))
          .verifyComplete();

      StepVerifier.create(sut.findIndexById(index.id()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsByTableIdAndName 메서드는")
  class ExistsByTableIdAndName {

    @Test
    @DisplayName("동일한 테이블과 이름이 있으면 true를 반환한다")
    void returnsTrueWhenExists() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.existsByTableIdAndName(TABLE_ID, index.name()))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("테이블이 다르면 false를 반환한다")
    void returnsFalseWhenDifferentTable() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.existsByTableIdAndName(OTHER_TABLE_ID, index.name()))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

    @Test
    @DisplayName("이름이 다르면 false를 반환한다")
    void returnsFalseWhenDifferentName() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.existsByTableIdAndName(TABLE_ID, "different_name"))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsByTableIdAndNameExcludingId 메서드는")
  class ExistsByTableIdAndNameExcludingId {

    @Test
    @DisplayName("다른 인덱스에서 동일한 이름이 있으면 true를 반환한다")
    void returnsTrueWhenExistsInOtherIndex() {
      var index1 = IndexFixture.indexWithIdAndName(INDEX_ID_1, "idx_test");
      var index2 = IndexFixture.indexWithIdAndName(INDEX_ID_2, "idx_other");
      sut.createIndex(index1).block();
      sut.createIndex(index2).block();

      StepVerifier.create(sut.existsByTableIdAndNameExcludingId(
          TABLE_ID, "idx_test", INDEX_ID_2))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("자기 자신의 이름이면 false를 반환한다")
    void returnsFalseWhenSameIndex() {
      var index = IndexFixture.defaultIndex();
      sut.createIndex(index).block();

      StepVerifier.create(sut.existsByTableIdAndNameExcludingId(
          TABLE_ID, index.name(), index.id()))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

}
