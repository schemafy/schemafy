package com.schemafy.api.erd.service;

import java.util.Map;

import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.erd.schema.domain.Schema;

public record SchemaSnapshotReadResult(
    Schema schema,
    long currentRevision,
    Map<String, TableSnapshotResponse> tableSnapshots) {
}
