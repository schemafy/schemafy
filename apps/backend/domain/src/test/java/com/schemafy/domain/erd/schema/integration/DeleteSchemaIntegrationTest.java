package com.schemafy.domain.erd.schema.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaUseCase;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdUseCase;

import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Schema Cascade 삭제 통합 테스트")
class DeleteSchemaIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  DeleteSchemaUseCase deleteSchemaUseCase;

  @Autowired
  GetSchemaUseCase getSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  @Nested
  @DisplayName("deleteSchema 메서드는")
  class DeleteSchema {

    @Nested
    @DisplayName("스키마에 테이블이 있을 때")
    class WithTables {

      private String schemaId;

      @BeforeEach
      void setUp() {
        var createSchemaCommand = new CreateSchemaCommand(
            PROJECT_ID, "MySQL", "integration_test_schema",
            "utf8mb4", "utf8mb4_general_ci");

        var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
        schemaId = schemaResult.id();

        var createTableCommand = new CreateTableCommand(
            schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
        createTableUseCase.createTable(createTableCommand).block();
      }

      @Test
      @DisplayName("스키마와 관련 테이블을 모두 삭제한다")
      void deletesSchemaAndRelatedTables() {
        StepVerifier.create(deleteSchemaUseCase.deleteSchema(
            new DeleteSchemaCommand(schemaId)))
            .verifyComplete();

        StepVerifier.create(getSchemaUseCase.getSchema(
            new GetSchemaQuery(schemaId)))
            .expectError(SchemaNotExistException.class)
            .verify();

        StepVerifier.create(getTablesBySchemaIdUseCase.getTablesBySchemaId(
            new GetTablesBySchemaIdQuery(schemaId))
            .collectList())
            .assertNext(tables -> assertThat(tables).isEmpty())
            .verifyComplete();
      }

    }

  }

}
