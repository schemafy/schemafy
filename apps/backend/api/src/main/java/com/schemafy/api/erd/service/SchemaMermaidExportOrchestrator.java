package com.schemafy.api.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.api.erd.controller.dto.response.SchemaMermaidExportResponse;
import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidCommand;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SchemaMermaidExportOrchestrator {

  private final SchemaExportSnapshotReader schemaExportSnapshotReader;
  private final GenerateSchemaMermaidUseCase generateSchemaMermaidUseCase;

  public Mono<SchemaMermaidExportResponse> exportSchemaMermaid(
      String schemaId) {
    return schemaExportSnapshotReader.readSchemaExportSnapshot(schemaId)
        .flatMap(result -> generateSchemaMermaidUseCase
            .generateSchemaMermaid(new GenerateSchemaMermaidCommand(
                result.snapshot()))
            .map(mermaid -> new SchemaMermaidExportResponse(
                result.snapshot().schema().id(),
                result.currentRevision(),
                mermaid)));
  }

}
