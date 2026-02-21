package com.schemafy.domain.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetRelationshipColumnService")
class GetRelationshipColumnServiceTest {

  @Mock
  GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  @InjectMocks
  GetRelationshipColumnService sut;

  @Nested
  @DisplayName("getRelationshipColumn 메서드는")
  class GetRelationshipColumn {

    @Test
    @DisplayName("존재하는 관계 컬럼을 반환한다")
    void returnsExistingColumn() {
      var query = RelationshipFixture.getRelationshipColumnQuery();
      var column = RelationshipFixture.defaultRelationshipColumn();

      given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
          .willReturn(Mono.just(column));

      StepVerifier.create(sut.getRelationshipColumn(query))
          .assertNext(result -> {
            assertThat(result.id()).isEqualTo(RelationshipFixture.DEFAULT_COLUMN_ID);
            assertThat(result.relationshipId()).isEqualTo(RelationshipFixture.DEFAULT_ID);
            assertThat(result.pkColumnId()).isEqualTo(RelationshipFixture.DEFAULT_PK_COLUMN_ID);
            assertThat(result.fkColumnId()).isEqualTo(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 예외가 발생한다")
    void throwsWhenNotExists() {
      var query = RelationshipFixture.getRelationshipColumnQuery();

      given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getRelationshipColumn(query))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.COLUMN_NOT_FOUND))
          .verify();
    }

  }

}
