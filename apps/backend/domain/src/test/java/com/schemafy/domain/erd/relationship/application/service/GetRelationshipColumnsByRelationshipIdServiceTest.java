package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetRelationshipColumnsByRelationshipIdService")
class GetRelationshipColumnsByRelationshipIdServiceTest {

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @InjectMocks
  GetRelationshipColumnsByRelationshipIdService sut;

  @Nested
  @DisplayName("getRelationshipColumnsByRelationshipId 메서드는")
  class GetRelationshipColumnsByRelationshipId {

    @Test
    @DisplayName("관계의 컬럼 목록을 반환한다")
    void returnsColumnsForRelationship() {
      var query = RelationshipFixture.getRelationshipColumnsByRelationshipIdQuery();
      var column = RelationshipFixture.defaultRelationshipColumn();

      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(column)));

      StepVerifier.create(sut.getRelationshipColumnsByRelationshipId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(RelationshipFixture.DEFAULT_COLUMN_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("컬럼이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoColumns() {
      var query = RelationshipFixture.getRelationshipColumnsByRelationshipIdQuery();

      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getRelationshipColumnsByRelationshipId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("seqNo 순서로 정렬된 컬럼 목록을 반환한다")
    void returnsColumnsInSeqNoOrder() {
      var query = RelationshipFixture.getRelationshipColumnsByRelationshipIdQuery();
      var col1 = RelationshipFixture.relationshipColumn("col1", RelationshipFixture.DEFAULT_ID, "pk1", "fk1", 0);
      var col2 = RelationshipFixture.relationshipColumn("col2", RelationshipFixture.DEFAULT_ID, "pk2", "fk2", 1);
      var col3 = RelationshipFixture.relationshipColumn("col3", RelationshipFixture.DEFAULT_ID, "pk3", "fk3", 2);

      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(col1, col2, col3)));

      StepVerifier.create(sut.getRelationshipColumnsByRelationshipId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(3);
            assertThat(result.get(0).seqNo()).isEqualTo(0);
            assertThat(result.get(1).seqNo()).isEqualTo(1);
            assertThat(result.get(2).seqNo()).isEqualTo(2);
          })
          .verifyComplete();
    }

  }

}
