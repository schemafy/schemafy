package com.schemafy.api.erd.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader;
import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader.SchemaExportSnapshotResult;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.type.IndexType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaDdlExportOrchestrator")
class SchemaDdlExportOrchestratorTest {

  @Mock
  SchemaExportSnapshotReader schemaExportSnapshotReader;

  @Mock
  GenerateSchemaDdlUseCase generateSchemaDdlUseCase;

  SchemaDdlExportOrchestrator sut;

  @BeforeEach
  void setUp() {
    sut = new SchemaDdlExportOrchestrator(
        schemaExportSnapshotReader,
        generateSchemaDdlUseCase);
  }

  @Test
  @DisplayName("공통 schema export snapshot을 DDL use case로 전달하고 응답을 반환한다")
  void exportsSchemaDdl() {
    String schemaId = "schema-1";
    SchemaExportSnapshot snapshot = new SchemaExportSnapshot(
        new SchemaSnapshot(schemaId, "mysql", "main_schema", null, null),
        List.of());
    IndexCapabilities indexCapabilities = mysqlIndexCapabilities();
    given(schemaExportSnapshotReader.readSchemaExportSnapshot(schemaId))
        .willReturn(Mono.just(new SchemaExportSnapshotResult(
            snapshot, 42L, indexCapabilities)));
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

    then(generateSchemaDdlUseCase).should().generateSchemaDdl(argThat(
        command -> command.snapshot() == snapshot
            && command.targetDbVendor().equals(DdlExportVendor.MYSQL)
            && command.indexCapabilities().equals(indexCapabilities)));
  }

  private static IndexCapabilities mysqlIndexCapabilities() {
    return new IndexCapabilities(
        Set.of(IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL),
        Set.of(IndexType.BTREE));
  }

}
