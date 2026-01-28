package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
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
@DisplayName("ConstraintPersistenceAdapter")
class ConstraintPersistenceAdapterTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String OTHER_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTH";
  private static final String TABLE_ID = ConstraintFixture.DEFAULT_TABLE_ID;
  private static final String OTHER_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTT";
  private static final String CONSTRAINT_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5CS1";
  private static final String CONSTRAINT_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5CS2";

  @Autowired
  ConstraintPersistenceAdapter sut;

  @Autowired
  ConstraintColumnPersistenceAdapter constraintColumnAdapter;

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
    databaseClient.sql("DELETE FROM db_tables").then().block();
    databaseClient.sql("DELETE FROM db_schemas").then().block();
  }

  @Nested
  @DisplayName("createConstraint 메서드는")
  class CreateConstraint {

    @Test
    @DisplayName("제약조건을 저장하고 반환한다")
    void savesAndReturnsConstraint() {
      var constraint = ConstraintFixture.defaultConstraint();

      StepVerifier.create(sut.createConstraint(constraint))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(constraint.id());
            assertThat(saved.tableId()).isEqualTo(constraint.tableId());
            assertThat(saved.name()).isEqualTo(constraint.name());
            assertThat(saved.kind()).isEqualTo(constraint.kind());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("PRIMARY KEY 제약조건을 저장한다")
    void savesPrimaryKeyConstraint() {
      var constraint = ConstraintFixture.primaryKeyConstraint();

      StepVerifier.create(sut.createConstraint(constraint))
          .assertNext(saved -> {
            assertThat(saved.kind()).isEqualTo(ConstraintKind.PRIMARY_KEY);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("UNIQUE 제약조건을 저장한다")
    void savesUniqueConstraint() {
      var constraint = ConstraintFixture.uniqueConstraint();

      StepVerifier.create(sut.createConstraint(constraint))
          .assertNext(saved -> {
            assertThat(saved.kind()).isEqualTo(ConstraintKind.UNIQUE);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("CHECK 제약조건을 checkExpr과 함께 저장한다")
    void savesCheckConstraintWithExpression() {
      var constraint = ConstraintFixture.checkConstraintWithExpr("value > 0");

      StepVerifier.create(sut.createConstraint(constraint))
          .assertNext(saved -> {
            assertThat(saved.kind()).isEqualTo(ConstraintKind.CHECK);
            assertThat(saved.checkExpr()).isEqualTo("value > 0");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("DEFAULT 제약조건을 defaultExpr과 함께 저장한다")
    void savesDefaultConstraintWithExpression() {
      var constraint = ConstraintFixture.defaultConstraintWithExpr("0");

      StepVerifier.create(sut.createConstraint(constraint))
          .assertNext(saved -> {
            assertThat(saved.kind()).isEqualTo(ConstraintKind.DEFAULT);
            assertThat(saved.defaultExpr()).isEqualTo("0");
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findConstraintById 메서드는")
  class FindConstraintById {

    @Test
    @DisplayName("존재하는 제약조건을 반환한다")
    void returnsExistingConstraint() {
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.findConstraintById(constraint.id()))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(constraint.id());
            assertThat(found.name()).isEqualTo(constraint.name());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findConstraintById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findConstraintsByTableId 메서드는")
  class FindConstraintsByTableId {

    @Test
    @DisplayName("해당 테이블의 제약조건들을 반환한다")
    void returnsConstraintsOfTable() {
      var constraint1 = ConstraintFixture.constraintWithId(CONSTRAINT_ID_1);
      var constraint2 = ConstraintFixture.constraintWithIdAndName(CONSTRAINT_ID_2, "uq_other");
      sut.createConstraint(constraint1).block();
      sut.createConstraint(constraint2).block();

      StepVerifier.create(sut.findConstraintsByTableId(TABLE_ID))
          .assertNext(constraints -> {
            assertThat(constraints).hasSize(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 테이블의 제약조건은 반환하지 않는다")
    void returnsOnlyConstraintsOfSpecifiedTable() {
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.findConstraintsByTableId(OTHER_TABLE_ID))
          .assertNext(constraints -> assertThat(constraints).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeConstraintName 메서드는")
  class ChangeConstraintName {

    @Test
    @DisplayName("제약조건 이름을 변경한다")
    void changesConstraintName() {
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();
      var newName = "new_pk_name";

      StepVerifier.create(sut.changeConstraintName(constraint.id(), newName))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintById(constraint.id()))
          .assertNext(found -> assertThat(found.name()).isEqualTo(newName))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 제약조건이면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      StepVerifier.create(sut.changeConstraintName("non-existent-id", "new_name"))
          .expectError(ConstraintNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("deleteConstraint 메서드는")
  class DeleteConstraint {

    @Test
    @DisplayName("제약조건을 삭제한다")
    void deletesConstraint() {
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.deleteConstraint(constraint.id()))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintById(constraint.id()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsBySchemaIdAndName 메서드는")
  class ExistsBySchemaIdAndName {

    @Test
    @DisplayName("동일한 스키마와 이름이 있으면 true를 반환한다")
    void returnsTrueWhenExists() {
      createSchema(SCHEMA_ID, "test_schema");
      createTable(TABLE_ID, SCHEMA_ID, "test_table");
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.existsBySchemaIdAndName(SCHEMA_ID, constraint.name()))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("스키마가 다르면 false를 반환한다")
    void returnsFalseWhenDifferentSchema() {
      createSchema(SCHEMA_ID, "test_schema");
      createSchema(OTHER_SCHEMA_ID, "other_schema");
      createTable(TABLE_ID, SCHEMA_ID, "test_table");
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.existsBySchemaIdAndName(OTHER_SCHEMA_ID, constraint.name()))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

    @Test
    @DisplayName("이름이 다르면 false를 반환한다")
    void returnsFalseWhenDifferentName() {
      createSchema(SCHEMA_ID, "test_schema");
      createTable(TABLE_ID, SCHEMA_ID, "test_table");
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.existsBySchemaIdAndName(SCHEMA_ID, "different_name"))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsBySchemaIdAndNameExcludingId 메서드는")
  class ExistsBySchemaIdAndNameExcludingId {

    @Test
    @DisplayName("다른 제약조건에서 동일한 이름이 있으면 true를 반환한다")
    void returnsTrueWhenExistsInOtherConstraint() {
      createSchema(SCHEMA_ID, "test_schema");
      createTable(TABLE_ID, SCHEMA_ID, "test_table");
      var constraint1 = ConstraintFixture.constraintWithIdAndName(CONSTRAINT_ID_1, "pk_test");
      var constraint2 = ConstraintFixture.constraintWithIdAndName(CONSTRAINT_ID_2, "uq_other");
      sut.createConstraint(constraint1).block();
      sut.createConstraint(constraint2).block();

      StepVerifier.create(sut.existsBySchemaIdAndNameExcludingId(
          SCHEMA_ID, "pk_test", CONSTRAINT_ID_2))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("자기 자신의 이름이면 false를 반환한다")
    void returnsFalseWhenSameConstraint() {
      createSchema(SCHEMA_ID, "test_schema");
      createTable(TABLE_ID, SCHEMA_ID, "test_table");
      var constraint = ConstraintFixture.defaultConstraint();
      sut.createConstraint(constraint).block();

      StepVerifier.create(sut.existsBySchemaIdAndNameExcludingId(
          SCHEMA_ID, constraint.name(), constraint.id()))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("cascadeDeleteByTableId 메서드는")
  class CascadeDeleteByTableId {

    @Test
    @DisplayName("테이블 삭제 시 관련 제약조건과 컬럼을 모두 삭제한다")
    void deletesConstraintsAndColumnsWhenTableDeleted() {
      var constraint = ConstraintFixture.defaultConstraint();
      var constraintColumn = ConstraintFixture.defaultConstraintColumn();
      sut.createConstraint(constraint).block();
      constraintColumnAdapter.createConstraintColumn(constraintColumn).block();

      StepVerifier.create(sut.cascadeDeleteByTableId(TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintById(constraint.id()))
          .verifyComplete();

      StepVerifier.create(constraintColumnAdapter.findConstraintColumnById(constraintColumn.id()))
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 제약조건이 있을 때 관련된 제약조건만 삭제한다")
    void deletesOnlyRelatedConstraints() {
      var constraint1 = ConstraintFixture.constraintWithId(CONSTRAINT_ID_1);
      var constraint2 = new Constraint(
          CONSTRAINT_ID_2, OTHER_TABLE_ID, "other_pk",
          ConstraintKind.PRIMARY_KEY, null, null);
      sut.createConstraint(constraint1).block();
      sut.createConstraint(constraint2).block();

      StepVerifier.create(sut.cascadeDeleteByTableId(TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintById(CONSTRAINT_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findConstraintById(CONSTRAINT_ID_2))
          .assertNext(found -> assertThat(found.id()).isEqualTo(CONSTRAINT_ID_2))
          .verifyComplete();
    }

  }

  private void createSchema(String schemaId, String name) {
    databaseClient.sql("""
        INSERT INTO db_schemas (id, project_id, db_vendor_name, name, charset, collation)
        VALUES (:id, :projectId, :dbVendorName, :name, :charset, :collation)
        """)
        .bind("id", schemaId)
        .bind("projectId", "01ARZ3NDEKTSV4RRFFQ69G5PRJ")
        .bind("dbVendorName", "MySQL")
        .bind("name", name)
        .bind("charset", "utf8mb4")
        .bind("collation", "utf8mb4_general_ci")
        .then()
        .block();
  }

  private void createTable(String tableId, String schemaId, String name) {
    databaseClient.sql("""
        INSERT INTO db_tables (id, schema_id, name, charset, collation)
        VALUES (:id, :schemaId, :name, :charset, :collation)
        """)
        .bind("id", tableId)
        .bind("schemaId", schemaId)
        .bind("name", name)
        .bind("charset", "utf8mb4")
        .bind("collation", "utf8mb4_general_ci")
        .then()
        .block();
  }

}
