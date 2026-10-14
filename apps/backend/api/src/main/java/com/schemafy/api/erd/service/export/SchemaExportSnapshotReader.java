package com.schemafy.api.erd.service.export;

import org.springframework.stereotype.Service;

import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.service.SchemaSnapshotReadResult;
import com.schemafy.api.erd.service.SchemaSnapshotReader;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SchemaExportSnapshotReader {

  private final SchemaSnapshotReader schemaSnapshotReader;
  private final GetProjectDbVendorUseCase getProjectDbVendorUseCase;
  private final SchemaExportSnapshotMapper snapshotMapper;

  public Mono<SchemaExportSnapshotResult> readSchemaExportSnapshot(
      String schemaId) {
    return schemaSnapshotReader.readSchemaSnapshot(schemaId)
        .flatMap(result -> getProjectDbVendorUseCase
            .getProjectDbVendor(
                new GetProjectDbVendorQuery(result.schema().projectId()))
            .map(dbVendor -> toExportSnapshotResult(result, dbVendor)));
  }

  private SchemaExportSnapshotResult toExportSnapshotResult(
      SchemaSnapshotReadResult result,
      DbVendor dbVendor) {
    SchemaResponse schema = SchemaResponse.from(
        result.schema(), result.currentRevision());
    return new SchemaExportSnapshotResult(
        snapshotMapper.toSnapshot(
            schema, result.tableSnapshots().values(), dbVendor.name()),
        result.currentRevision(),
        dbVendor.datatypeMappings(),
        dbVendor.capabilities().indexes(),
        dbVendor.capabilities().identifiers());
  }

  public record SchemaExportSnapshotResult(
      SchemaExportSnapshot snapshot,
      long currentRevision,
      DatatypePolicy datatypePolicy,
      IndexCapabilities indexCapabilities,
      IdentifierCapabilities identifierCapabilities) {
  }

}
