package com.schemafy.domain.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.schema.application.port.out.CreateSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;
import com.schemafy.domain.erd.schema.fixture.SchemaFixture;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSchemaService")
class CreateSchemaServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateSchemaPort createSchemaPort;

  @Mock
  SchemaExistsPort schemaExistsPort;

  @InjectMocks
  CreateSchemaService sut;

  @Nested
  @DisplayName("createSchema 메서드는")
  class CreateSchema {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("스키마를 생성하고 결과를 반환한다")
      void returnsCreatedSchema() {
        var command = SchemaFixture.createCommand();
        var schema = SchemaFixture.defaultSchema();

        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(ulidGeneratorPort.generate())
            .willReturn(SchemaFixture.DEFAULT_ID);
        given(createSchemaPort.createSchema(any(Schema.class)))
            .willReturn(Mono.just(schema));

        StepVerifier.create(sut.createSchema(command))
            .assertNext(result -> {
              assertThat(result.id()).isEqualTo(SchemaFixture.DEFAULT_ID);
              assertThat(result.projectId()).isEqualTo(command.projectId());
              assertThat(result.dbVendorName()).isEqualTo(command.dbVendorName());
              assertThat(result.name()).isEqualTo(command.name());
              assertThat(result.charset()).isEqualTo(command.charset());
              assertThat(result.collation()).isEqualTo(command.collation());
            })
            .verifyComplete();

        then(schemaExistsPort).should()
            .existsActiveByProjectIdAndName(command.projectId(), command.name());
        then(createSchemaPort).should().createSchema(any(Schema.class));
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("SchemaNameDuplicateException을 발생시킨다")
      void throwsSchemaNameDuplicateException() {
        var command = SchemaFixture.createCommand();

        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.createSchema(command))
            .expectError(SchemaNameDuplicateException.class)
            .verify();

        then(createSchemaPort).shouldHaveNoInteractions();
        then(ulidGeneratorPort).shouldHaveNoInteractions();
      }

    }

  }

}
