package com.schemafy.api.erd.service;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.erd.service.ddl.DdlExportSnapshotMapper;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionResult;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.domain.DbVendor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaDdlExportOrchestrator")
class SchemaDdlExportOrchestratorTest {

  @Mock
  GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;

  @Mock
  GetProjectDbVendorUseCase getProjectDbVendorUseCase;

  @Mock
  GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;

  @Mock
  TableSnapshotOrchestrator tableSnapshotOrchestrator;

  @Mock
  GenerateSchemaDdlUseCase generateSchemaDdlUseCase;

  @Mock
  ReactiveTransactionManager transactionManager;

  @Mock
  ReactiveTransaction transaction;

  SchemaDdlExportOrchestrator sut;

  @BeforeEach
  void setUp() {
    given(transactionManager.getReactiveTransaction(any()))
        .willReturn(Mono.just(transaction));
    lenient().when(transactionManager.commit(transaction))
        .thenReturn(Mono.empty());
    lenient().when(transactionManager.rollback(transaction))
        .thenReturn(Mono.empty());

    sut = new SchemaDdlExportOrchestrator(
        getSchemaWithRevisionUseCase,
        getProjectDbVendorUseCase,
        getTablesBySchemaIdUseCase,
        tableSnapshotOrchestrator,
        generateSchemaDdlUseCase,
        new DdlExportSnapshotMapper(),
        transactionManager);
  }

  @Test
  @DisplayName("schema snapshot을 core DDL use case로 전달하고 export 응답을 반환한다")
  void exportsSchemaDdl() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    Table table = new Table("table-1", schemaId, "users", "utf8mb4",
        "utf8mb4_general_ci");
    TableSnapshotResponse snapshot = new TableSnapshotResponse(
        new TableResponse(table.id(), schemaId, table.name(), table.charset(),
            table.collation(), null),
        List.of(new ColumnResponse(
            "column-1", table.id(), "id", "BIGINT", null, 0, true,
            null, null, null)),
        List.of(),
        List.of(),
        List.of());

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 42L)));
    given(getProjectDbVendorUseCase.getProjectDbVendor(
        new GetProjectDbVendorQuery(schema.projectId())))
        .willReturn(Mono.just(sourceDbVendor()));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table));
    given(tableSnapshotOrchestrator.getTableSnapshotsStrict(anyList()))
        .willReturn(Mono.just(Map.of(table.id(), snapshot)));
    given(generateSchemaDdlUseCase.generateSchemaDdl(any(GenerateSchemaDdlCommand.class)))
        .willReturn(Mono.just("DDL"));

    StepVerifier.create(sut.exportSchemaDdl(schemaId, " MySQL "))
        .assertNext(response -> {
          assertThat(response.schemaId()).isEqualTo(schemaId);
          assertThat(response.currentRevision()).isEqualTo(42L);
          assertThat(response.targetDbVendor()).isEqualTo("mysql");
          assertThat(response.ddl()).isEqualTo("DDL");
        })
        .verifyComplete();

    then(getSchemaWithRevisionUseCase).should()
        .getSchemaWithRevision(new GetSchemaQuery(schemaId));
    then(getProjectDbVendorUseCase).should()
        .getProjectDbVendor(new GetProjectDbVendorQuery(schema.projectId()));
    then(getTablesBySchemaIdUseCase).should()
        .getTablesBySchemaId(new GetTablesBySchemaIdQuery(schemaId));
    then(generateSchemaDdlUseCase).should().generateSchemaDdl(argThat(
        command -> command.snapshot().schema().id().equals(schemaId)
            && command.snapshot().schema().dbVendorName().equals("mariadb")
            && command.targetDbVendor().equals(DdlExportVendor.MYSQL)
            && command.snapshot().tables().size() == 1
            && command.snapshot().tables().getFirst().columns().size() == 1));
  }

  @Test
  @DisplayName("테이블이 없어도 빈 table snapshot으로 DDL 생성을 요청한다")
  void exportsEmptySchemaDdl() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "empty_schema",
        "utf8mb4", "utf8mb4_general_ci");

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 7L)));
    given(getProjectDbVendorUseCase.getProjectDbVendor(
        new GetProjectDbVendorQuery(schema.projectId())))
        .willReturn(Mono.just(sourceDbVendor()));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.empty());
    given(generateSchemaDdlUseCase.generateSchemaDdl(any(GenerateSchemaDdlCommand.class)))
        .willReturn(Mono.just("EMPTY DDL"));

    StepVerifier.create(sut.exportSchemaDdl(schemaId, "mysql"))
        .assertNext(response -> {
          assertThat(response.currentRevision()).isEqualTo(7L);
          assertThat(response.targetDbVendor()).isEqualTo("mysql");
          assertThat(response.ddl()).isEqualTo("EMPTY DDL");
        })
        .verifyComplete();

    then(tableSnapshotOrchestrator).shouldHaveNoInteractions();
    then(generateSchemaDdlUseCase).should().generateSchemaDdl(argThat(
        command -> command.snapshot().tables().isEmpty()
            && command.targetDbVendor().equals(DdlExportVendor.MYSQL)));
  }

  private static DbVendor sourceDbVendor() {
    return new DbVendor(
        1L,
        "MariaDB 11",
        "mariadb",
        "11",
        "{}");
  }

}
