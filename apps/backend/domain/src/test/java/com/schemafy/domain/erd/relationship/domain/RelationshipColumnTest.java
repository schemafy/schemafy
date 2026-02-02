package com.schemafy.domain.erd.relationship.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RelationshipColumn")
class RelationshipColumnTest {

  @Nested
  @DisplayName("생성 시")
  class WhenCreating {

    @Test
    @DisplayName("모든 필드를 설정할 수 있다")
    void createsWithAllFields() {
      var column = RelationshipFixture.defaultRelationshipColumn();

      assertThat(column.id()).isEqualTo(RelationshipFixture.DEFAULT_COLUMN_ID);
      assertThat(column.relationshipId()).isEqualTo(RelationshipFixture.DEFAULT_ID);
      assertThat(column.pkColumnId()).isEqualTo(RelationshipFixture.DEFAULT_PK_COLUMN_ID);
      assertThat(column.fkColumnId()).isEqualTo(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
      assertThat(column.seqNo()).isEqualTo(RelationshipFixture.DEFAULT_SEQ_NO);
    }

    @Test
    @DisplayName("seqNo가 0일 수 있다")
    void allowsZeroSeqNo() {
      var column = RelationshipFixture.relationshipColumnWithSeqNo(0);

      assertThat(column.seqNo()).isZero();
    }

    @Test
    @DisplayName("seqNo가 양수일 수 있다")
    void allowsPositiveSeqNo() {
      var column = RelationshipFixture.relationshipColumnWithSeqNo(5);

      assertThat(column.seqNo()).isEqualTo(5);
    }

    @Test
    @DisplayName("다른 relationshipId로 생성할 수 있다")
    void createsWithDifferentRelationshipId() {
      String otherRelationshipId = "01ARZ3NDEKTSV4RRFFQ69G5OTH";
      var column = RelationshipFixture.relationshipColumnWithRelationshipId(otherRelationshipId);

      assertThat(column.relationshipId()).isEqualTo(otherRelationshipId);
    }

  }

  @Nested
  @DisplayName("동등성 비교 시")
  class WhenComparingEquality {

    @Test
    @DisplayName("같은 값을 가진 관계 컬럼은 동등하다")
    void columnsWithSameValuesAreEqual() {
      var column1 = RelationshipFixture.defaultRelationshipColumn();
      var column2 = RelationshipFixture.defaultRelationshipColumn();

      assertThat(column1).isEqualTo(column2);
    }

    @Test
    @DisplayName("다른 id를 가진 관계 컬럼은 동등하지 않다")
    void columnsWithDifferentIdsAreNotEqual() {
      var column1 = RelationshipFixture.relationshipColumnWithId("id1");
      var column2 = RelationshipFixture.relationshipColumnWithId("id2");

      assertThat(column1).isNotEqualTo(column2);
    }

    @Test
    @DisplayName("다른 seqNo를 가진 관계 컬럼은 동등하지 않다")
    void columnsWithDifferentSeqNosAreNotEqual() {
      var column1 = RelationshipFixture.relationshipColumnWithSeqNo(0);
      var column2 = RelationshipFixture.relationshipColumnWithSeqNo(1);

      assertThat(column1).isNotEqualTo(column2);
    }

  }

}
