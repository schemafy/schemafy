package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetRelationshipsByTableIdService")
class GetRelationshipsByTableIdServiceTest {

  @Mock
  GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;

  @InjectMocks
  GetRelationshipsByTableIdService sut;

  @Nested
  @DisplayName("getRelationshipsByTableId 메서드는")
  class GetRelationshipsByTableId {

    @Test
    @DisplayName("테이블의 관계 목록을 반환한다")
    void returnsRelationshipsForTable() {
      var query = RelationshipFixture.getRelationshipsByTableIdQuery();
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
          .willReturn(Mono.just(List.of(relationship)));

      StepVerifier.create(sut.getRelationshipsByTableId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(RelationshipFixture.DEFAULT_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("관계가 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoRelationships() {
      var query = RelationshipFixture.getRelationshipsByTableIdQuery();

      given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getRelationshipsByTableId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("FK, PK 양쪽 관계를 모두 반환한다")
    void returnsBothFkAndPkRelationships() {
      var query = RelationshipFixture.getRelationshipsByTableIdQuery();
      var fkRelationship = RelationshipFixture.relationshipWithIdAndName("rel1", "fk_rel");
      var pkRelationship = RelationshipFixture.relationshipWithIdAndName("rel2", "pk_rel");

      given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
          .willReturn(Mono.just(List.of(fkRelationship, pkRelationship)));

      StepVerifier.create(sut.getRelationshipsByTableId(query))
          .assertNext(result -> assertThat(result).hasSize(2))
          .verifyComplete();
    }

  }

}
