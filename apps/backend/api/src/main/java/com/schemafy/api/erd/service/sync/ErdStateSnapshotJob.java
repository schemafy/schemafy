package com.schemafy.api.erd.service.sync;

public record ErdStateSnapshotJob(
    String jobKey,
    String projectId,
    String schemaId,
    ErdStateSnapshotJobKind kind,
    long targetRevision,
    long generation,
    String leaseToken,
    int failureCount) {
}
