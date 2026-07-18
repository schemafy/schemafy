package com.schemafy.api.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.api.erd.controller.dto.response.SchemaDdlExportResponse;
import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;

import reactor.core.publisher.Mono;

@Service
public class SchemaDdlExportOrchestrator {

  private final SchemaExportSnapshotReader schemaExportSnapshotReader;
  private final GenerateSchemaDdlUseCase generateSchemaDdlUseCase;

  public SchemaDdlExportOrchestrator(
      SchemaExportSnapshotReader schemaExportSnapshotReader,
      GenerateSchemaDdlUseCase generateSchemaDdlUseCase) {
    this.schemaExportSnapshotReader = schemaExportSnapshotReader;
    this.generateSchemaDdlUseCase = generateSchemaDdlUseCase;
  }

  public Mono<SchemaDdlExportResponse> exportSchemaDdl(String schemaId,
      String targetDbVendor) {
    return Mono.defer(() -> {
      DdlExportVendor exportVendor = DdlExportVendor.of(targetDbVendor);
      return schemaExportSnapshotReader.readSchemaExportSnapshot(schemaId)
          .flatMap(result -> generateSchemaDdlUseCase
              .generateSchemaDdl(new GenerateSchemaDdlCommand(
                  result.snapshot(), exportVendor))
              .map(ddl -> new SchemaDdlExportResponse(
                  result.snapshot().schema().id(),
                  result.currentRevision(),
                  exportVendor.value(),
                  ddl)));
    });
  }

}
