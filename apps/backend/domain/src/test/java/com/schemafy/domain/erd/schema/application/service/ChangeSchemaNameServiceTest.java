package com.schemafy.domain.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.schema.fixture.SchemaFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeSchemaNameService")
class ChangeSchemaNameServiceTest {

  @Mock
  ChangeSchemaNamePort changeSchemaNamePort;

  @Mock
  SchemaExistsPort schemaExistsPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @InjectMocks
  ChangeSchemaNameService sut;

  @Nested
  @DisplayName("changeSchemaName 메서드는")
  class ChangeSchemaName {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("스키마 이름을 변경한다")
      void changesSchemaName() {
        var newName = "new_schema_name";
        var command = SchemaFixture.changeNameCommand(newName);

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(changeSchemaNamePort.changeSchemaName(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeSchemaName(command))
            .expectNextCount(1)
            .verifyComplete();

        then(schemaExistsPort).should()
            .existsActiveByProjectIdAndName(command.projectId(), command.newName());
        then(changeSchemaNamePort).should()
            .changeSchemaName(command.schemaId(), command.newName());
      }

    }

    @Nested
    @DisplayName("요청 projectId와 스키마 projectId가 다르면")
    class WithProjectMismatch {

      @Test
      @DisplayName("SchemaNotExistException을 발생시킨다")
      void throwsSchemaNotExistException() {
        var command = SchemaFixture.changeNameCommand("new_schema_name");
        var schema = new Schema(
            command.schemaId(),
            "other-project-id",
            SchemaFixture.DEFAULT_DB_VENDOR,
            SchemaFixture.DEFAULT_NAME,
            SchemaFixture.DEFAULT_CHARSET,
            SchemaFixture.DEFAULT_COLLATION);

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(schema));

        StepVerifier.create(sut.changeSchemaName(command))
            .expectError(SchemaNotExistException.class)
            .verify();

        then(schemaExistsPort).shouldHaveNoInteractions();
        then(changeSchemaNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("SchemaNameDuplicateException을 발생시킨다")
      void throwsSchemaNameDuplicateException() {
        var newName = "duplicate_name";
        var command = SchemaFixture.changeNameCommand(newName);

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.changeSchemaName(command))
            .expectError(SchemaNameDuplicateException.class)
            .verify();

        then(changeSchemaNamePort).shouldHaveNoInteractions();
      }

    }

  }

}
