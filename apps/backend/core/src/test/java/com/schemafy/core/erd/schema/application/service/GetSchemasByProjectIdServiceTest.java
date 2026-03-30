package com.schemafy.core.erd.schema.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.core.erd.schema.application.port.out.ActiveProjectExistsPort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemasByProjectIdPort;
import com.schemafy.core.erd.schema.fixture.SchemaFixture;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSchemasByProjectIdService")
class GetSchemasByProjectIdServiceTest {

  @Mock
  ActiveProjectExistsPort activeProjectExistsPort;

  @Mock
  GetSchemasByProjectIdPort getSchemasByProjectIdPort;

  @InjectMocks
  GetSchemasByProjectIdService sut;

  @Nested
  @DisplayName("getSchemasByProjectId 메서드는")
  class GetSchemasByProjectId {

    @Nested
    @DisplayName("활성 프로젝트가 존재하면")
    class WhenProjectExists {

      @Test
      @DisplayName("스키마 목록을 반환한다")
      void returnsSchemas() {
        var query = new GetSchemasByProjectIdQuery(SchemaFixture.DEFAULT_PROJECT_ID);
        var schema1 = SchemaFixture.defaultSchema();
        var schema2 = SchemaFixture.schemaWithId("01ARZ3NDEKTSV4RRFFQ69G5FAX");

        given(activeProjectExistsPort.existsActiveProjectById(any()))
            .willReturn(Mono.just(true));
        given(getSchemasByProjectIdPort.findSchemasByProjectId(any()))
            .willReturn(Flux.just(schema1, schema2));

        StepVerifier.create(sut.getSchemasByProjectId(query).collectList())
            .assertNext(schemas -> {
              assertThat(schemas).hasSize(2);
              assertThat(schemas).extracting(schema -> schema.projectId())
                  .containsOnly(query.projectId());
            })
            .verifyComplete();

        then(activeProjectExistsPort).should()
            .existsActiveProjectById(query.projectId());
        then(getSchemasByProjectIdPort).should()
            .findSchemasByProjectId(query.projectId());
      }

      @Test
      @DisplayName("스키마가 없으면 빈 목록을 반환한다")
      void returnsEmptyListWhenSchemasDoNotExist() {
        var query = new GetSchemasByProjectIdQuery(SchemaFixture.DEFAULT_PROJECT_ID);

        given(activeProjectExistsPort.existsActiveProjectById(any()))
            .willReturn(Mono.just(true));
        given(getSchemasByProjectIdPort.findSchemasByProjectId(any()))
            .willReturn(Flux.empty());

        StepVerifier.create(sut.getSchemasByProjectId(query).collectList())
            .assertNext(schemas -> assertThat(schemas).isEqualTo(List.of()))
            .verifyComplete();

        then(activeProjectExistsPort).should()
            .existsActiveProjectById(query.projectId());
        then(getSchemasByProjectIdPort).should()
            .findSchemasByProjectId(query.projectId());
      }

    }

    @Nested
    @DisplayName("프로젝트가 존재하지 않으면")
    class WithoutProject {

      @Test
      @DisplayName("ProjectNotFoundException을 발생시킨다")
      void throwsProjectNotFoundException() {
        var query = new GetSchemasByProjectIdQuery(SchemaFixture.DEFAULT_PROJECT_ID);

        given(activeProjectExistsPort.existsActiveProjectById(any()))
            .willReturn(Mono.just(false));

        StepVerifier.create(sut.getSchemasByProjectId(query))
            .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
            .verify();

        then(activeProjectExistsPort).should()
            .existsActiveProjectById(query.projectId());
        then(getSchemasByProjectIdPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("프로젝트가 삭제된 상태이면")
    class WithDeletedProject {

      @Test
      @DisplayName("ProjectNotFoundException을 발생시킨다")
      void throwsProjectNotFoundException() {
        var query = new GetSchemasByProjectIdQuery(SchemaFixture.DEFAULT_PROJECT_ID);

        given(activeProjectExistsPort.existsActiveProjectById(any()))
            .willReturn(Mono.just(false));

        StepVerifier.create(sut.getSchemasByProjectId(query))
            .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
            .verify();

        then(activeProjectExistsPort).should()
            .existsActiveProjectById(query.projectId());
        then(getSchemasByProjectIdPort).shouldHaveNoInteractions();
      }

    }

  }

}
