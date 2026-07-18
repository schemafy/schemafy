package com.schemafy.api.erd.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader;
import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader.SchemaExportSnapshotResult;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidCommand;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaMermaidExportOrchestrator")
class SchemaMermaidExportOrchestratorTest {

  @Mock
  SchemaExportSnapshotReader schemaExportSnapshotReader;

  @Mock
  GenerateSchemaMermaidUseCase generateSchemaMermaidUseCase;

  SchemaMermaidExportOrchestrator sut;

  @BeforeEach
  void setUp() {
    sut = new SchemaMermaidExportOrchestrator(
        schemaExportSnapshotReader,
        generateSchemaMermaidUseCase);
  }

  @Test
  @DisplayName("공통 schema export snapshot으로 Mermaid 응답을 생성한다")
  void exportsSchemaMermaid() {
    String schemaId = "schema-1";
    SchemaExportSnapshot snapshot = new SchemaExportSnapshot(
        new SchemaSnapshot(schemaId, "mysql", "main_schema", null, null),
        List.of());
    given(schemaExportSnapshotReader.readSchemaExportSnapshot(schemaId))
        .willReturn(Mono.just(new SchemaExportSnapshotResult(snapshot, 42L)));
    given(generateSchemaMermaidUseCase.generateSchemaMermaid(
        any(GenerateSchemaMermaidCommand.class)))
        .willReturn(Mono.just("erDiagram"));

    StepVerifier.create(sut.exportSchemaMermaid(schemaId))
        .assertNext(response -> {
          assertThat(response.schemaId()).isEqualTo(schemaId);
          assertThat(response.currentRevision()).isEqualTo(42L);
          assertThat(response.mermaid()).isEqualTo("erDiagram");
        })
        .verifyComplete();

    then(generateSchemaMermaidUseCase).should().generateSchemaMermaid(argThat(
        command -> command.snapshot() == snapshot));
  }

}
