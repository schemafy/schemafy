package com.schemafy.core.erd.schema.integration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.support.ErdProjectIntegrationSupport;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Schema 생성 통합 테스트")
class CreateSchemaIntegrationTest extends ErdProjectIntegrationSupport {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Test
  @DisplayName("활성 프로젝트에 대해 스키마를 생성한다")
  void createsSchemaForActiveProject() {
    String projectId = createActiveProjectId("schema_create");
    String schemaName = "schema_create_" + UUID.randomUUID().toString().substring(0, 8);

    StepVerifier.create(createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        schemaName,
        "utf8mb4",
        "utf8mb4_general_ci")))
        .assertNext(result -> {
          assertThat(result.result().projectId()).isEqualTo(projectId);
          assertThat(result.result().name()).isEqualTo(schemaName);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("존재하지 않는 프로젝트면 생성이 실패한다")
  void failsWhenProjectDoesNotExist() {
    String missingProjectId = UlidGenerator.generate();

    StepVerifier.create(createSchemaUseCase.createSchema(new CreateSchemaCommand(
        missingProjectId,
        "MySQL",
        "missing_project_schema",
        "utf8mb4",
        "utf8mb4_general_ci")))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();
  }

  @Test
  @DisplayName("삭제된 프로젝트면 생성이 실패한다")
  void failsWhenProjectIsDeleted() {
    String projectId = createActiveProjectId("schema_deleted_project");
    softDeleteProject(projectId);

    StepVerifier.create(createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "deleted_project_schema",
        "utf8mb4",
        "utf8mb4_general_ci")))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();
  }

}
