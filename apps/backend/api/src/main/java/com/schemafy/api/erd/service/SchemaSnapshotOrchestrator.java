package com.schemafy.api.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.SchemaSnapshotsResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SchemaSnapshotOrchestrator {

  private final SchemaSnapshotReader schemaSnapshotReader;

  public Mono<SchemaSnapshotsResponse> getSchemaSnapshots(String schemaId) {
    return getSchemaState(schemaId)
        .map(state -> new SchemaSnapshotsResponse(state.revision(),
            state.snapshots()));
  }

  public Mono<SchemaStateSnapshot> getSchemaState(String schemaId) {
    return schemaSnapshotReader.readSchemaSnapshot(schemaId)
        .map(result -> new SchemaStateSnapshot(
            SchemaResponse.from(result.schema()),
            result.currentRevision(),
            result.tableSnapshots()));
  }

}
