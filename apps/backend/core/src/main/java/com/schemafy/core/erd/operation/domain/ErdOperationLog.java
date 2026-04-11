package com.schemafy.core.erd.operation.domain;

public record ErdOperationLog(
    String opId,
    String projectId,
    String schemaId,
    ErdOperationType opType,
    long committedRevision,
    Long baseSchemaRevision,
    String clientOperationId,
    String collabSessionId,
    String actorUserId,
    ErdOperationDerivationKind derivationKind,
    String derivedFromOpId,
    ErdOperationLifecycleState lifecycleState,
    String payloadJson,
    String inversePayloadJson,
    String touchedEntitiesJson,
    String affectedTableIdsJson) {
}
