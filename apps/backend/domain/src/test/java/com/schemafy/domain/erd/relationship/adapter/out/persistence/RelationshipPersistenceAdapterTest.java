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
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
  RelationshipPersistenceAdapter.class,
  RelationshipColumnPersistenceAdapter.class,
  RelationshipMapper.class,
  RelationshipColumnMapper.class,
  R2dbcTestConfiguration.class
})
@DisplayName("RelationshipPersistenceAdapter")
class RelationshipPersistenceAdapterTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String OTHER_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTH";
  private static final String PK_TABLE_ID = RelationshipFixture.DEFAULT_PK_TABLE_ID;
  private static final String FK_TABLE_ID = RelationshipFixture.DEFAULT_FK_TABLE_ID;
  private static final String OTHER_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTT";
  private static final String RELATIONSHIP_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5RL1";
  private static final String RELATIONSHIP_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5RL2";

  @Autowired
  RelationshipPersistenceAdapter sut;

  @Autowired
  RelationshipColumnPersistenceAdapter relationshipColumnAdapter;

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
    databaseClient.sql("DELETE FROM db_tables").then().block();
  }

  @Nested
  @DisplayName("createRelationship 메서드는")
  class CreateRelationship {

    @Test
    @DisplayName("관계를 저장하고 반환한다")
    void savesAndReturnsRelationship() {
      var relationship = RelationshipFixture.defaultRelationship();

      StepVerifier.create(sut.createRelationship(relationship))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(relationship.id());
            assertThat(saved.pkTableId()).isEqualTo(relationship.pkTableId());
            assertThat(saved.fkTableId()).isEqualTo(relationship.fkTableId());
            assertThat(saved.name()).isEqualTo(relationship.name());
            assertThat(saved.kind()).isEqualTo(relationship.kind());
            assertThat(saved.cardinality()).isEqualTo(relationship.cardinality());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("IDENTIFYING 관계를 저장한다")
    void savesIdentifyingRelationship() {
      var relationship = RelationshipFixture.identifyingRelationship();

      StepVerifier.create(sut.createRelationship(relationship))
          .assertNext(saved -> {
            assertThat(saved.kind()).isEqualTo(RelationshipKind.IDENTIFYING);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("extra 필드가 있는 관계를 저장한다")
    void savesRelationshipWithExtra() {
      var relationship = RelationshipFixture.relationshipWithExtra("{\"description\": \"test\"}");

      StepVerifier.create(sut.createRelationship(relationship))
          .assertNext(saved -> {
            assertThat(saved.extra()).isEqualTo("{\"description\": \"test\"}");
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findRelationshipById 메서드는")
  class FindRelationshipById {

    @Test
    @DisplayName("존재하는 관계를 반환한다")
    void returnsExistingRelationship() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(relationship.id());
            assertThat(found.name()).isEqualTo(relationship.name());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findRelationshipById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findRelationshipsBySchemaId 메서드는")
  class FindRelationshipsBySchemaId {

    @BeforeEach
    void setUpTables() {
      createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");
      createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      createTable(OTHER_TABLE_ID, OTHER_SCHEMA_ID, "other_table");
    }

    @Test
    @DisplayName("해당 스키마의 관계들을 반환한다")
    void returnsRelationshipsOfSchema() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.findRelationshipsBySchemaId(SCHEMA_ID))
          .assertNext(relationships -> {
            assertThat(relationships).hasSize(1);
            assertThat(relationships.get(0).id()).isEqualTo(relationship.id());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 스키마의 관계는 반환하지 않는다")
    void returnsOnlyRelationshipsOfSpecifiedSchema() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.findRelationshipsBySchemaId(OTHER_SCHEMA_ID))
          .assertNext(relationships -> assertThat(relationships).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findRelationshipsByTableId 메서드는")
  class FindRelationshipsByTableId {

    @Test
    @DisplayName("FK 테이블로 관계를 조회한다")
    void findsRelationshipsByFkTable() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.findRelationshipsByTableId(FK_TABLE_ID))
          .assertNext(relationships -> {
            assertThat(relationships).hasSize(1);
            assertThat(relationships.get(0).fkTableId()).isEqualTo(FK_TABLE_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("PK 테이블로 관계를 조회한다")
    void findsRelationshipsByPkTable() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.findRelationshipsByTableId(PK_TABLE_ID))
          .assertNext(relationships -> {
            assertThat(relationships).hasSize(1);
            assertThat(relationships.get(0).pkTableId()).isEqualTo(PK_TABLE_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("관련 없는 테이블이면 빈 리스트를 반환한다")
    void returnsEmptyWhenNoRelatedRelationships() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.findRelationshipsByTableId(OTHER_TABLE_ID))
          .assertNext(relationships -> assertThat(relationships).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeRelationshipName 메서드는")
  class ChangeRelationshipName {

    @Test
    @DisplayName("관계 이름을 변경한다")
    void changesRelationshipName() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();
      var newName = "new_fk_name";

      StepVerifier.create(sut.changeRelationshipName(relationship.id(), newName))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .assertNext(found -> assertThat(found.name()).isEqualTo(newName))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      StepVerifier.create(sut.changeRelationshipName("non-existent-id", "new_name"))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("changeRelationshipKind 메서드는")
  class ChangeRelationshipKind {

    @Test
    @DisplayName("관계 Kind를 변경한다")
    void changesRelationshipKind() {
      var relationship = RelationshipFixture.nonIdentifyingRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.changeRelationshipKind(relationship.id(), RelationshipKind.IDENTIFYING))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .assertNext(found -> assertThat(found.kind()).isEqualTo(RelationshipKind.IDENTIFYING))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      StepVerifier.create(sut.changeRelationshipKind("non-existent-id", RelationshipKind.IDENTIFYING))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("changeRelationshipCardinality 메서드는")
  class ChangeRelationshipCardinality {

    @Test
    @DisplayName("관계 Cardinality를 변경한다")
    void changesRelationshipCardinality() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.changeRelationshipCardinality(relationship.id(), Cardinality.ONE_TO_ONE))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .assertNext(found -> assertThat(found.cardinality()).isEqualTo(Cardinality.ONE_TO_ONE))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      StepVerifier.create(sut.changeRelationshipCardinality("non-existent-id", Cardinality.ONE_TO_ONE))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("changeRelationshipExtra 메서드는")
  class ChangeRelationshipExtra {

    @Test
    @DisplayName("관계 Extra를 변경한다")
    void changesRelationshipExtra() {
      var relationship = RelationshipFixture.defaultRelationship();
      var created = sut.createRelationship(relationship).block();
      assertThat(created.extra()).isNull();

      var newExtra = "{\"updated\": true}";

      sut.changeRelationshipExtra(relationship.id(), newExtra).block();

      var found = sut.findRelationshipById(relationship.id()).block();
      assertThat(found).isNotNull();
      // H2 JSON 타입은 문자열을 이중 이스케이프할 수 있어 contains로 검증
      assertThat(found.extra()).isNotNull();
      assertThat(found.extra()).contains("updated");
      assertThat(found.extra()).contains("true");
    }

    @Test
    @DisplayName("Extra를 null로 변경한다")
    void changesExtraToNull() {
      var relationship = RelationshipFixture.relationshipWithExtra("{\"initial\": true}");
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.changeRelationshipExtra(relationship.id(), null))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .assertNext(found -> assertThat(found.extra()).isNull())
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      StepVerifier.create(sut.changeRelationshipExtra("non-existent-id", "{}"))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("deleteRelationship 메서드는")
  class DeleteRelationship {

    @Test
    @DisplayName("관계를 삭제한다")
    void deletesRelationship() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.deleteRelationship(relationship.id()))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsByFkTableIdAndName 메서드는")
  class ExistsByFkTableIdAndName {

    @Test
    @DisplayName("동일한 FK 테이블과 이름이 있으면 true를 반환한다")
    void returnsTrueWhenExists() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.existsByFkTableIdAndName(FK_TABLE_ID, relationship.name()))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("FK 테이블이 다르면 false를 반환한다")
    void returnsFalseWhenDifferentFkTable() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.existsByFkTableIdAndName(OTHER_TABLE_ID, relationship.name()))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

    @Test
    @DisplayName("이름이 다르면 false를 반환한다")
    void returnsFalseWhenDifferentName() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.existsByFkTableIdAndName(FK_TABLE_ID, "different_name"))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsByFkTableIdAndNameExcludingId 메서드는")
  class ExistsByFkTableIdAndNameExcludingId {

    @Test
    @DisplayName("다른 관계에서 동일한 이름이 있으면 true를 반환한다")
    void returnsTrueWhenExistsInOtherRelationship() {
      var relationship1 = RelationshipFixture.relationshipWithIdAndName(RELATIONSHIP_ID_1, "fk_name");
      var relationship2 = new Relationship(
          RELATIONSHIP_ID_2, PK_TABLE_ID, FK_TABLE_ID, "other_fk_name",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
      sut.createRelationship(relationship1).block();
      sut.createRelationship(relationship2).block();

      StepVerifier.create(sut.existsByFkTableIdAndNameExcludingId(
          FK_TABLE_ID, "fk_name", RELATIONSHIP_ID_2))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("자기 자신의 이름이면 false를 반환한다")
    void returnsFalseWhenSameRelationship() {
      var relationship = RelationshipFixture.defaultRelationship();
      sut.createRelationship(relationship).block();

      StepVerifier.create(sut.existsByFkTableIdAndNameExcludingId(
          FK_TABLE_ID, relationship.name(), relationship.id()))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("cascadeDeleteByTableId 메서드는")
  class CascadeDeleteByTableId {

    @Test
    @DisplayName("FK 테이블 삭제 시 관련 관계와 컬럼을 모두 삭제한다")
    void deletesRelationshipsAndColumnsWhenFkTableDeleted() {
      var relationship = RelationshipFixture.defaultRelationship();
      var relationshipColumn = RelationshipFixture.defaultRelationshipColumn();
      sut.createRelationship(relationship).block();
      relationshipColumnAdapter.createRelationshipColumn(relationshipColumn).block();

      StepVerifier.create(sut.cascadeDeleteByTableId(FK_TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .verifyComplete();

      StepVerifier.create(relationshipColumnAdapter.findRelationshipColumnById(relationshipColumn.id()))
          .verifyComplete();
    }

    @Test
    @DisplayName("PK 테이블 삭제 시 관련 관계와 컬럼을 모두 삭제한다")
    void deletesRelationshipsAndColumnsWhenPkTableDeleted() {
      var relationship = RelationshipFixture.defaultRelationship();
      var relationshipColumn = RelationshipFixture.defaultRelationshipColumn();
      sut.createRelationship(relationship).block();
      relationshipColumnAdapter.createRelationshipColumn(relationshipColumn).block();

      StepVerifier.create(sut.cascadeDeleteByTableId(PK_TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(relationship.id()))
          .verifyComplete();

      StepVerifier.create(relationshipColumnAdapter.findRelationshipColumnById(relationshipColumn.id()))
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 관계가 있을 때 관련된 관계만 삭제한다")
    void deletesOnlyRelatedRelationships() {
      var relationship1 = RelationshipFixture.relationshipWithId(RELATIONSHIP_ID_1);
      var relationship2 = new Relationship(
          RELATIONSHIP_ID_2, OTHER_TABLE_ID, OTHER_TABLE_ID, "other_fk",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
      sut.createRelationship(relationship1).block();
      sut.createRelationship(relationship2).block();

      StepVerifier.create(sut.cascadeDeleteByTableId(FK_TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(RELATIONSHIP_ID_1))
          .verifyComplete();

      StepVerifier.create(sut.findRelationshipById(RELATIONSHIP_ID_2))
          .assertNext(found -> assertThat(found.id()).isEqualTo(RELATIONSHIP_ID_2))
          .verifyComplete();
    }

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
