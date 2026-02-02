package com.schemafy.domain.erd.relationship.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Relationship")
class RelationshipTest {

  @Nested
  @DisplayName("생성 시")
  class WhenCreating {

    @Test
    @DisplayName("모든 필드를 설정할 수 있다")
    void createsWithAllFields() {
      var relationship = RelationshipFixture.defaultRelationship();

      assertThat(relationship.id()).isEqualTo(RelationshipFixture.DEFAULT_ID);
      assertThat(relationship.pkTableId()).isEqualTo(RelationshipFixture.DEFAULT_PK_TABLE_ID);
      assertThat(relationship.fkTableId()).isEqualTo(RelationshipFixture.DEFAULT_FK_TABLE_ID);
      assertThat(relationship.name()).isEqualTo(RelationshipFixture.DEFAULT_NAME);
      assertThat(relationship.kind()).isEqualTo(RelationshipFixture.DEFAULT_KIND);
      assertThat(relationship.cardinality()).isEqualTo(RelationshipFixture.DEFAULT_CARDINALITY);
      assertThat(relationship.extra()).isNull();
    }

    @Test
    @DisplayName("extra 필드는 null을 허용한다")
    void allowsNullExtra() {
      var relationship = RelationshipFixture.defaultRelationship();

      assertThat(relationship.extra()).isNull();
    }

    @Test
    @DisplayName("extra 필드에 값을 설정할 수 있다")
    void allowsExtraValue() {
      var extra = "Some extra information";
      var relationship = RelationshipFixture.relationshipWithExtra(extra);

      assertThat(relationship.extra()).isEqualTo(extra);
    }

    @Test
    @DisplayName("IDENTIFYING 타입으로 생성할 수 있다")
    void createsIdentifyingRelationship() {
      var relationship = RelationshipFixture.identifyingRelationship();

      assertThat(relationship.kind()).isEqualTo(RelationshipKind.IDENTIFYING);
    }

    @Test
    @DisplayName("NON_IDENTIFYING 타입으로 생성할 수 있다")
    void createsNonIdentifyingRelationship() {
      var relationship = RelationshipFixture.nonIdentifyingRelationship();

      assertThat(relationship.kind()).isEqualTo(RelationshipKind.NON_IDENTIFYING);
    }

    @Test
    @DisplayName("ONE_TO_ONE 카디널리티로 생성할 수 있다")
    void createsOneToOneRelationship() {
      var relationship = RelationshipFixture.relationshipWithCardinality(Cardinality.ONE_TO_ONE);

      assertThat(relationship.cardinality()).isEqualTo(Cardinality.ONE_TO_ONE);
    }

    @Test
    @DisplayName("ONE_TO_MANY 카디널리티로 생성할 수 있다")
    void createsOneToManyRelationship() {
      var relationship = RelationshipFixture.relationshipWithCardinality(Cardinality.ONE_TO_MANY);

      assertThat(relationship.cardinality()).isEqualTo(Cardinality.ONE_TO_MANY);
    }

  }

  @Nested
  @DisplayName("동등성 비교 시")
  class WhenComparingEquality {

    @Test
    @DisplayName("같은 값을 가진 관계는 동등하다")
    void relationshipsWithSameValuesAreEqual() {
      var relationship1 = RelationshipFixture.defaultRelationship();
      var relationship2 = RelationshipFixture.defaultRelationship();

      assertThat(relationship1).isEqualTo(relationship2);
    }

    @Test
    @DisplayName("다른 id를 가진 관계는 동등하지 않다")
    void relationshipsWithDifferentIdsAreNotEqual() {
      var relationship1 = RelationshipFixture.relationshipWithId("id1");
      var relationship2 = RelationshipFixture.relationshipWithId("id2");

      assertThat(relationship1).isNotEqualTo(relationship2);
    }

  }

}
