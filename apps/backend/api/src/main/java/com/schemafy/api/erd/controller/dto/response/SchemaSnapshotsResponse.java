package com.schemafy.api.erd.controller.dto.response;

import java.util.Map;

public record SchemaSnapshotsResponse(
    long currentRevision,
    Map<String, TableSnapshotResponse> snapshots) {
}
