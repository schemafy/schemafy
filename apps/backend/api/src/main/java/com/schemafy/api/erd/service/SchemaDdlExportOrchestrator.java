package com.schemafy.api.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.api.erd.controller.dto.response.SchemaDdlExportResponse;
import com.schemafy.api.erd.service.export.SchemaExportSnapshotReader;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SchemaDdlExportOrchestrator {

  private final SchemaExportSnapshotReader schemaExportSnapshotReader;
  private final GenerateSchemaDdlUseCase generateSchemaDdlUseCase;

  public Mono<SchemaDdlExportResponse> exportSchemaDdl(String schemaId,
      String targetDbVendor) {
    return Mono.defer(() -> {
      DdlExportVendor exportVendor = DdlExportVendor.of(targetDbVendor);
      return schemaExportSnapshotReader.readSchemaExportSnapshot(schemaId)
          .flatMap(result -> {
            requireMatchingTargetVendor(
                exportVendor, result.snapshot().schema().dbVendorName());
            return generateSchemaDdlUseCase
                .generateSchemaDdl(new GenerateSchemaDdlCommand(
                    result.snapshot(),
                    exportVendor,
                    result.indexCapabilities(),
                    result.identifierCapabilities()))
                .map(ddl -> new SchemaDdlExportResponse(
                    result.snapshot().schema().id(),
                    result.currentRevision(),
                    exportVendor.value(),
                    ddl));
          });
    });
  }

  private static void requireMatchingTargetVendor(
      DdlExportVendor targetDbVendor,
      String projectDbVendorName) {
    DdlExportVendor projectDbVendor = DdlExportVendor.of(projectDbVendorName);
    if (!targetDbVendor.equals(projectDbVendor)) {
      throw new DomainException(
          DdlErrorCode.UNSUPPORTED_VENDOR,
          "DDL export target DB vendor must match the project's DB vendor: "
              + projectDbVendor.value());
    }
  }

}
