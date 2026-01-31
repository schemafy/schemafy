package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
  ConstraintPersistenceAdapter.class,
  ConstraintColumnPersistenceAdapter.class,
  ConstraintMapper.class,
  ConstraintColumnMapper.class,
  R2dbcTestConfiguration.class
})
@DisplayName("ConstraintColumnPersistenceAdapter")
class ConstraintColumnPersistenceAdapterTest {

  private static final String CONSTRAINT_ID_1 = ConstraintFixture.DEFAULT_ID;
  private static final String CONSTRAINT_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5CS2";
  private static final String COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5CC1";
  private static final String COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5CC2";
  private static final String COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5CC3";
  private static final String TABLE_COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5TC1";
  private static final String TABLE_COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5TC2";
  private static final String TABLE_COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5TC3";

  @Autowired
  ConstraintColumnPersistenceAdapter sut;

  @Autowired
  ConstraintPersistenceAdapter constraintAdapter;

  @Autowired
  ConstraintRepository constraintRepository;

  @Autowired
  ConstraintColumnRepository constraintColumnRepository;

  @Autowired
  DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    constraintColumnRepository.deleteAll().block();
    constraintRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("createConstraintColumn 메서드는")
  class CreateConstraintColumn {

    @BeforeEach
    void setUpConstraint() {
      var constraint = ConstraintFixture.defaultConstraint();
      constraintAdapter.createConstraint(constraint).block();
    }

    @Test
    @DisplayName("제약조건 컬럼을 저장하고 반환한다")
    void savesAndReturnsConstraintColumn() {
      var column = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);

      StepVerifier.create(sut.createConstraintColumn(column))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(COLUMN_ID_1);
            assertThat(saved.constraintId()).isEqualTo(CONSTRAINT_ID_1);
            assertThat(saved.columnId()).isEqualTo(TABLE_COLUMN_ID_1);
            assertThat(saved.seqNo()).isEqualTo(0);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("seqNo가 다른 여러 제약조건 컬럼을 저장한다")
    void savesMultipleColumnsWithDifferentSeqNo() {
      var column1 = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      var column2 = ConstraintFixture.constraintColumn(
          COLUMN_ID_2, CONSTRAINT_ID_1, TABLE_COLUMN_ID_2, 1);

      sut.createConstraintColumn(column1).block();
      sut.createConstraintColumn(column2).block();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(2);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findConstraintColumnById 메서드는")
  class FindConstraintColumnById {

    @BeforeEach
    void setUpConstraint() {
      var constraint = ConstraintFixture.defaultConstraint();
      constraintAdapter.createConstraint(constraint).block();
    }

    @Test
    @DisplayName("존재하는 제약조건 컬럼을 반환한다")
    void returnsExistingConstraintColumn() {
      var column = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      sut.createConstraintColumn(column).block();

      StepVerifier.create(sut.findConstraintColumnById(COLUMN_ID_1))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(COLUMN_ID_1);
            assertThat(found.constraintId()).isEqualTo(CONSTRAINT_ID_1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findConstraintColumnById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findConstraintColumnsByConstraintId 메서드는")
  class FindConstraintColumnsByConstraintId {

    @BeforeEach
    void setUpConstraints() {
      var constraint1 = ConstraintFixture.constraintWithId(CONSTRAINT_ID_1);
      var constraint2 = ConstraintFixture.constraintWithIdAndName(CONSTRAINT_ID_2, "uq_other");
      constraintAdapter.createConstraint(constraint1).block();
      constraintAdapter.createConstraint(constraint2).block();
    }

    @Test
    @DisplayName("해당 제약조건의 컬럼들을 seqNo 순으로 반환한다")
    void returnsColumnsOrderedBySeqNo() {
      var column1 = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 2);
      var column2 = ConstraintFixture.constraintColumn(
          COLUMN_ID_2, CONSTRAINT_ID_1, TABLE_COLUMN_ID_2, 0);
      var column3 = ConstraintFixture.constraintColumn(
          COLUMN_ID_3, CONSTRAINT_ID_1, TABLE_COLUMN_ID_3, 1);
      sut.createConstraintColumn(column1).block();
      sut.createConstraintColumn(column2).block();
      sut.createConstraintColumn(column3).block();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_2); // seqNo 0
            assertThat(columns.get(1).id()).isEqualTo(COLUMN_ID_3); // seqNo 1
            assertThat(columns.get(2).id()).isEqualTo(COLUMN_ID_1); // seqNo 2
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 제약조건의 컬럼은 반환하지 않는다")
    void returnsOnlyColumnsOfSpecifiedConstraint() {
      var column1 = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      var column2 = ConstraintFixture.constraintColumn(
          COLUMN_ID_2, CONSTRAINT_ID_2, TABLE_COLUMN_ID_2, 0);
      sut.createConstraintColumn(column1).block();
      sut.createConstraintColumn(column2).block();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("제약조건에 컬럼이 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoColumns() {
      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeConstraintColumnPositions 메서드는")
  class ChangeConstraintColumnPositions {

    @BeforeEach
    void setUpConstraintWithColumns() {
      var constraint = ConstraintFixture.defaultConstraint();
      constraintAdapter.createConstraint(constraint).block();

      var column1 = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      var column2 = ConstraintFixture.constraintColumn(
          COLUMN_ID_2, CONSTRAINT_ID_1, TABLE_COLUMN_ID_2, 1);
      var column3 = ConstraintFixture.constraintColumn(
          COLUMN_ID_3, CONSTRAINT_ID_1, TABLE_COLUMN_ID_3, 2);
      sut.createConstraintColumn(column1).block();
      sut.createConstraintColumn(column2).block();
      sut.createConstraintColumn(column3).block();
    }

    @Test
    @DisplayName("컬럼 위치를 변경한다")
    void changesColumnPositions() {
      var reorderedColumns = List.of(
          new ConstraintColumn(COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 2),
          new ConstraintColumn(COLUMN_ID_2, CONSTRAINT_ID_1, TABLE_COLUMN_ID_2, 0),
          new ConstraintColumn(COLUMN_ID_3, CONSTRAINT_ID_1, TABLE_COLUMN_ID_3, 1));

      StepVerifier.create(sut.changeConstraintColumnPositions(CONSTRAINT_ID_1, reorderedColumns))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
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
      StepVerifier.create(sut.changeConstraintColumnPositions(CONSTRAINT_ID_1, List.of()))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
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
      StepVerifier.create(sut.changeConstraintColumnPositions(CONSTRAINT_ID_1, null))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteConstraintColumn 메서드는")
  class DeleteConstraintColumn {

    @BeforeEach
    void setUpConstraintWithColumn() {
      var constraint = ConstraintFixture.defaultConstraint();
      constraintAdapter.createConstraint(constraint).block();

      var column = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      sut.createConstraintColumn(column).block();
    }

    @Test
    @DisplayName("제약조건 컬럼을 삭제한다")
    void deletesConstraintColumn() {
      StepVerifier.create(sut.deleteConstraintColumn(COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnById(COLUMN_ID_1))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 컬럼을 삭제해도 에러가 발생하지 않는다")
    void doesNotThrowWhenColumnNotExists() {
      StepVerifier.create(sut.deleteConstraintColumn("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteByConstraintId 메서드는")
  class DeleteByConstraintId {

    @BeforeEach
    void setUpConstraintsWithColumns() {
      var constraint1 = ConstraintFixture.constraintWithId(CONSTRAINT_ID_1);
      var constraint2 = ConstraintFixture.constraintWithIdAndName(CONSTRAINT_ID_2, "uq_other");
      constraintAdapter.createConstraint(constraint1).block();
      constraintAdapter.createConstraint(constraint2).block();

      var column1 = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      var column2 = ConstraintFixture.constraintColumn(
          COLUMN_ID_2, CONSTRAINT_ID_1, TABLE_COLUMN_ID_2, 1);
      var column3 = ConstraintFixture.constraintColumn(
          COLUMN_ID_3, CONSTRAINT_ID_2, TABLE_COLUMN_ID_3, 0);
      sut.createConstraintColumn(column1).block();
      sut.createConstraintColumn(column2).block();
      sut.createConstraintColumn(column3).block();
    }

    @Test
    @DisplayName("해당 제약조건의 모든 컬럼을 삭제한다")
    void deletesAllColumnsOfConstraint() {
      StepVerifier.create(sut.deleteByConstraintId(CONSTRAINT_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_1))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 제약조건의 컬럼은 삭제하지 않는다")
    void doesNotDeleteColumnsOfOtherConstraint() {
      StepVerifier.create(sut.deleteByConstraintId(CONSTRAINT_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnsByConstraintId(CONSTRAINT_ID_2))
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
    void setUpConstraintWithColumns() {
      var constraint = ConstraintFixture.defaultConstraint();
      constraintAdapter.createConstraint(constraint).block();

      var column1 = ConstraintFixture.constraintColumn(
          COLUMN_ID_1, CONSTRAINT_ID_1, TABLE_COLUMN_ID_1, 0);
      var column2 = ConstraintFixture.constraintColumn(
          COLUMN_ID_2, CONSTRAINT_ID_1, TABLE_COLUMN_ID_2, 1);
      sut.createConstraintColumn(column1).block();
      sut.createConstraintColumn(column2).block();
    }

    @Test
    @DisplayName("해당 테이블 컬럼을 참조하는 제약조건 컬럼을 삭제한다")
    void deletesConstraintColumnsByColumnId() {
      StepVerifier.create(sut.deleteByColumnId(TABLE_COLUMN_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintColumnById(COLUMN_ID_1))
          .verifyComplete();

      // 다른 테이블 컬럼을 참조하는 제약조건 컬럼은 남아있다
      StepVerifier.create(sut.findConstraintColumnById(COLUMN_ID_2))
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
