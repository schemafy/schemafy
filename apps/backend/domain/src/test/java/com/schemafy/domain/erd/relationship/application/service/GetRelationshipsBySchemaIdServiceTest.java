package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetRelationshipsBySchemaIdService")
class GetRelationshipsBySchemaIdServiceTest {

  @Mock
  GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  @InjectMocks
  GetRelationshipsBySchemaIdService sut;

  @Nested
  @DisplayName("getRelationshipsBySchemaId 메서드는")
  class GetRelationshipsBySchemaId {

    @Test
    @DisplayName("스키마의 관계 목록을 반환한다")
    void returnsRelationshipsForSchema() {
      var query = RelationshipFixture.getRelationshipsBySchemaIdQuery();
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of(relationship)));

      StepVerifier.create(sut.getRelationshipsBySchemaId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(RelationshipFixture.DEFAULT_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("관계가 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoRelationships() {
      var query = RelationshipFixture.getRelationshipsBySchemaIdQuery();

      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getRelationshipsBySchemaId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 테이블의 관계를 모두 반환한다")
    void returnsRelationshipsFromMultipleTables() {
      var query = RelationshipFixture.getRelationshipsBySchemaIdQuery();
      var rel1 = RelationshipFixture.relationshipWithIdAndName("rel1", "fk_1");
      var rel2 = RelationshipFixture.relationshipWithIdAndName("rel2", "fk_2");
      var rel3 = RelationshipFixture.relationshipWithIdAndName("rel3", "fk_3");

      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of(rel1, rel2, rel3)));

      StepVerifier.create(sut.getRelationshipsBySchemaId(query))
          .assertNext(result -> assertThat(result).hasSize(3))
          .verifyComplete();
    }

  }

}
