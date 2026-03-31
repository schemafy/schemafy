package com.schemafy.core.erd.schema.application.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.schema.fixture.SchemaFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSchemaWithRevisionService")
class GetSchemaWithRevisionServiceTest {

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;

  @InjectMocks
  GetSchemaWithRevisionService sut;

  @Nested
  @DisplayName("getSchemaWithRevision 메서드는")
  class GetSchemaWithRevision {

    @Test
    @DisplayName("스키마와 collaboration state가 존재하면 현재 revision과 함께 반환한다")
    void returnsSchemaWithCurrentRevision() {
      var query = new GetSchemaQuery(SchemaFixture.DEFAULT_ID);
      var schema = SchemaFixture.defaultSchema();
      var state = new SchemaCollaborationState(
          schema.id(),
          schema.projectId(),
          42L,
          Instant.parse("2026-01-01T00:00:00Z"),
          Instant.parse("2026-01-01T00:00:00Z"));

      given(getSchemaByIdPort.findSchemaById(any()))
          .willReturn(Mono.just(schema));
      given(findSchemaCollaborationStatePort.findBySchemaId(any()))
          .willReturn(Mono.just(state));

      StepVerifier.create(sut.getSchemaWithRevision(query))
          .assertNext(result -> {
            assertThat(result.schema().id()).isEqualTo(schema.id());
            assertThat(result.currentRevision()).isEqualTo(42L);
          })
          .verifyComplete();

      then(getSchemaByIdPort).should().findSchemaById(query.schemaId());
      then(findSchemaCollaborationStatePort).should().findBySchemaId(query.schemaId());
    }

    @Test
    @DisplayName("collaboration state가 없으면 revision 0과 함께 반환한다")
    void returnsSchemaWithZeroRevisionWhenStateMissing() {
      var query = new GetSchemaQuery(SchemaFixture.DEFAULT_ID);
      var schema = SchemaFixture.defaultSchema();

      given(getSchemaByIdPort.findSchemaById(any()))
          .willReturn(Mono.just(schema));
      given(findSchemaCollaborationStatePort.findBySchemaId(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getSchemaWithRevision(query))
          .assertNext(result -> {
            assertThat(result.schema().id()).isEqualTo(schema.id());
            assertThat(result.currentRevision()).isZero();
          })
          .verifyComplete();

      then(getSchemaByIdPort).should().findSchemaById(query.schemaId());
      then(findSchemaCollaborationStatePort).should().findBySchemaId(query.schemaId());
    }

    @Test
    @DisplayName("스키마가 존재하지 않으면 NOT_FOUND 예외를 발생시킨다")
    void throwsNotFoundWhenSchemaMissing() {
      var query = new GetSchemaQuery("non-existent-id");

      given(getSchemaByIdPort.findSchemaById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getSchemaWithRevision(query))
          .expectErrorMatches(DomainException.hasErrorCode(SchemaErrorCode.NOT_FOUND))
          .verify();

      then(getSchemaByIdPort).should().findSchemaById(query.schemaId());
      then(findSchemaCollaborationStatePort).shouldHaveNoInteractions();
    }

  }

}
