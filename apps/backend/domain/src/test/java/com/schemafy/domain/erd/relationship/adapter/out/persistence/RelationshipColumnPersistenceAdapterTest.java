package com.schemafy.domain.erd.relationship.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
    RelationshipPersistenceAdapter.class,
    RelationshipColumnPersistenceAdapter.class,
    RelationshipMapper.class,
    RelationshipColumnMapper.class,
    R2dbcTestConfiguration.class
})
@DisplayName("RelationshipColumnPersistenceAdapter")
class RelationshipColumnPersistenceAdapterTest {

  private static final String RELATIONSHIP_ID_1 = RelationshipFixture.DEFAULT_ID;
  private static final String RELATIONSHIP_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5RL2";
  private static final String COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5RC1";
  private static final String COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5RC2";
  private static final String COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5RC3";
  private static final String PK_COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5PK1";
  private static final String PK_COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5PK2";
  private static final String PK_COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5PK3";
  private static final String FK_COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5FK1";
  private static final String FK_COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5FK2";
  private static final String FK_COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5FK3";

  @Autowired
  RelationshipColumnPersistenceAdapter sut;

  @Autowired
  RelationshipPersistenceAdapter relationshipAdapter;

  @Autowired
  RelationshipRepository relationshipRepository;

  @Autowired
  RelationshipColumnRepository relationshipColumnRepository;

  @Autowired
  DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    relationshipColumnRepository.deleteAll().block();
    relationshipRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("createRelationshipColumn 메서드는")
  class CreateRelationshipColumn {

    @BeforeEach
    void setUpRelationship() {
      var relationship = RelationshipFixture.defaultRelationship();
      relationshipAdapter.createRelationship(relationship).block();
    }

    @Test
    @DisplayName("관계 컬럼을 저장하고 반환한다")
    void savesAndReturnsRelationshipColumn() {
      var column = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);

      StepVerifier.create(sut.createRelationshipColumn(column))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(COLUMN_ID_1);
            assertThat(saved.relationshipId()).isEqualTo(RELATIONSHIP_ID_1);
            assertThat(saved.pkColumnId()).isEqualTo(PK_COLUMN_ID_1);
            assertThat(saved.fkColumnId()).isEqualTo(FK_COLUMN_ID_1);
            assertThat(saved.seqNo()).isEqualTo(0);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("seqNo가 다른 여러 관계 컬럼을 저장한다")
    void savesMultipleColumnsWithDifferentSeqNo() {
      var column1 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      var column2 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_2, RELATIONSHIP_ID_1, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 1);

      sut.createRelationshipColumn(column1).block();
      sut.createRelationshipColumn(column2).block();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(2);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findRelationshipColumnById 메서드는")
  class FindRelationshipColumnById {

    @BeforeEach
    void setUpRelationship() {
      var relationship = RelationshipFixture.defaultRelationship();
      relationshipAdapter.createRelationship(relationship).block();
    }

    @Test
    @DisplayName("존재하는 관계 컬럼을 반환한다")
    void returnsExistingRelationshipColumn() {
      var column = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      sut.createRelationshipColumn(column).block();

      StepVerifier.create(sut.findRelationshipColumnById(COLUMN_ID_1))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(COLUMN_ID_1);
            assertThat(found.relationshipId()).isEqualTo(RELATIONSHIP_ID_1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findRelationshipColumnById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findRelationshipColumnsByRelationshipId 메서드는")
  class FindRelationshipColumnsByRelationshipId {

    @BeforeEach
    void setUpRelationships() {
      var relationship1 = RelationshipFixture.relationshipWithId(RELATIONSHIP_ID_1);
      var relationship2 = RelationshipFixture.relationshipWithIdAndName(RELATIONSHIP_ID_2, "fk_other");
      relationshipAdapter.createRelationship(relationship1).block();
      relationshipAdapter.createRelationship(relationship2).block();
    }

    @Test
    @DisplayName("해당 관계의 컬럼들을 seqNo 순으로 반환한다")
    void returnsColumnsOrderedBySeqNo() {
      var column1 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 2);
      var column2 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_2, RELATIONSHIP_ID_1, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 0);
      var column3 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_3, RELATIONSHIP_ID_1, PK_COLUMN_ID_3, FK_COLUMN_ID_3, 1);
      sut.createRelationshipColumn(column1).block();
      sut.createRelationshipColumn(column2).block();
      sut.createRelationshipColumn(column3).block();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_2); // seqNo 0
            assertThat(columns.get(1).id()).isEqualTo(COLUMN_ID_3); // seqNo 1
            assertThat(columns.get(2).id()).isEqualTo(COLUMN_ID_1); // seqNo 2
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 관계의 컬럼은 반환하지 않는다")
    void returnsOnlyColumnsOfSpecifiedRelationship() {
      var column1 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      var column2 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_2, RELATIONSHIP_ID_2, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 0);
      sut.createRelationshipColumn(column1).block();
      sut.createRelationshipColumn(column2).block();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("관계에 컬럼이 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoColumns() {
      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeRelationshipColumnPositions 메서드는")
  class ChangeRelationshipColumnPositions {

    @BeforeEach
    void setUpRelationshipWithColumns() {
      var relationship = RelationshipFixture.defaultRelationship();
      relationshipAdapter.createRelationship(relationship).block();

      var column1 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      var column2 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_2, RELATIONSHIP_ID_1, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 1);
      var column3 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_3, RELATIONSHIP_ID_1, PK_COLUMN_ID_3, FK_COLUMN_ID_3, 2);
      sut.createRelationshipColumn(column1).block();
      sut.createRelationshipColumn(column2).block();
      sut.createRelationshipColumn(column3).block();
    }

    @Test
    @DisplayName("컬럼 위치를 변경한다")
    void changesColumnPositions() {
      var reorderedColumns = List.of(
          new RelationshipColumn(COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 2),
          new RelationshipColumn(COLUMN_ID_2, RELATIONSHIP_ID_1, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 0),
          new RelationshipColumn(COLUMN_ID_3, RELATIONSHIP_ID_1, PK_COLUMN_ID_3, FK_COLUMN_ID_3, 1)
      );

      StepVerifier.create(sut.changeRelationshipColumnPositions(RELATIONSHIP_ID_1, reorderedColumns))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
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
      StepVerifier.create(sut.changeRelationshipColumnPositions(RELATIONSHIP_ID_1, List.of()))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
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
      StepVerifier.create(sut.changeRelationshipColumnPositions(RELATIONSHIP_ID_1, null))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteRelationshipColumn 메서드는")
  class DeleteRelationshipColumn {

    @BeforeEach
    void setUpRelationshipWithColumn() {
      var relationship = RelationshipFixture.defaultRelationship();
      relationshipAdapter.createRelationship(relationship).block();

      var column = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      sut.createRelationshipColumn(column).block();
    }

    @Test
    @DisplayName("관계 컬럼을 삭제한다")
    void deletesRelationshipColumn() {
      StepVerifier.create(sut.deleteRelationshipColumn(COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnById(COLUMN_ID_1))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 컬럼을 삭제해도 에러가 발생하지 않는다")
    void doesNotThrowWhenColumnNotExists() {
      StepVerifier.create(sut.deleteRelationshipColumn("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteByRelationshipId 메서드는")
  class DeleteByRelationshipId {

    @BeforeEach
    void setUpRelationshipsWithColumns() {
      var relationship1 = RelationshipFixture.relationshipWithId(RELATIONSHIP_ID_1);
      var relationship2 = RelationshipFixture.relationshipWithIdAndName(RELATIONSHIP_ID_2, "fk_other");
      relationshipAdapter.createRelationship(relationship1).block();
      relationshipAdapter.createRelationship(relationship2).block();

      var column1 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      var column2 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_2, RELATIONSHIP_ID_1, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 1);
      var column3 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_3, RELATIONSHIP_ID_2, PK_COLUMN_ID_3, FK_COLUMN_ID_3, 0);
      sut.createRelationshipColumn(column1).block();
      sut.createRelationshipColumn(column2).block();
      sut.createRelationshipColumn(column3).block();
    }

    @Test
    @DisplayName("해당 관계의 모든 컬럼을 삭제한다")
    void deletesAllColumnsOfRelationship() {
      StepVerifier.create(sut.deleteByRelationshipId(RELATIONSHIP_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_1))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 관계의 컬럼은 삭제하지 않는다")
    void doesNotDeleteColumnsOfOtherRelationship() {
      StepVerifier.create(sut.deleteByRelationshipId(RELATIONSHIP_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID_2))
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
    void setUpRelationshipWithColumns() {
      var relationship = RelationshipFixture.defaultRelationship();
      relationshipAdapter.createRelationship(relationship).block();

      // 같은 FK 컬럼을 참조하는 관계 컬럼들
      var column1 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_1, RELATIONSHIP_ID_1, PK_COLUMN_ID_1, FK_COLUMN_ID_1, 0);
      var column2 = RelationshipFixture.relationshipColumn(
          COLUMN_ID_2, RELATIONSHIP_ID_1, PK_COLUMN_ID_2, FK_COLUMN_ID_2, 1);
      sut.createRelationshipColumn(column1).block();
      sut.createRelationshipColumn(column2).block();
    }

    @Test
    @DisplayName("해당 FK 컬럼을 참조하는 관계 컬럼을 삭제한다")
    void deletesRelationshipColumnsByFkColumnId() {
      StepVerifier.create(sut.deleteByColumnId(FK_COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnById(COLUMN_ID_1))
          .verifyComplete();

      // 다른 FK 컬럼을 참조하는 관계 컬럼은 남아있다
      StepVerifier.create(sut.findRelationshipColumnById(COLUMN_ID_2))
          .assertNext(found -> assertThat(found.id()).isEqualTo(COLUMN_ID_2))
          .verifyComplete();
    }

    @Test
    @DisplayName("해당 PK 컬럼을 참조하는 관계 컬럼을 삭제한다")
    void deletesRelationshipColumnsByPkColumnId() {
      StepVerifier.create(sut.deleteByColumnId(PK_COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipColumnById(COLUMN_ID_1))
          .verifyComplete();

      // 다른 PK 컬럼을 참조하는 관계 컬럼은 남아있다
      StepVerifier.create(sut.findRelationshipColumnById(COLUMN_ID_2))
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
