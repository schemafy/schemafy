package com.schemafy.core.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
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
@DisplayName("GetSchemaService")
class GetSchemaServiceTest {

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @InjectMocks
  GetSchemaService sut;

  @Nested
  @DisplayName("getSchema 메서드는")
  class GetSchema {

    @Nested
    @DisplayName("스키마가 존재하면")
    class WhenSchemaExists {

      @Test
      @DisplayName("스키마를 반환한다")
      void returnsSchema() {
        var query = new GetSchemaQuery(SchemaFixture.DEFAULT_ID);
        var schema = SchemaFixture.defaultSchema();

        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));

        StepVerifier.create(sut.getSchema(query))
            .assertNext(result -> {
              assertThat(result.id()).isEqualTo(query.schemaId());
              assertThat(result.name()).isEqualTo(SchemaFixture.DEFAULT_NAME);
            })
            .verifyComplete();

        then(getSchemaByIdPort).should().findSchemaById(query.schemaId());
      }

    }

    @Nested
    @DisplayName("스키마가 존재하지 않으면")
    class WhenSchemaNotExists {

      @Test
      @DisplayName("SchemaNotExistException을 발생시킨다")
      void throwsSchemaNotExistException() {
        var query = new GetSchemaQuery("non-existent-id");

        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.getSchema(query))
            .expectErrorMatches(DomainException.hasErrorCode(SchemaErrorCode.NOT_FOUND))
            .verify();

        then(getSchemaByIdPort).should().findSchemaById(query.schemaId());
      }

    }

  }

}
