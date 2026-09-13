package com.schemafy.api.erd.service;

import java.util.Map;

import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;

public record SchemaStateSnapshot(
    SchemaResponse schema,
    long revision,
    Map<String, TableSnapshotResponse> snapshots) {
}
