package com.schemafy.core.erd.operation;

public record ErdOperationMetadata(
    String sessionId,
    String clientOperationId,
    Long baseSchemaRevision,
    String actorUserId) {

  public static ErdOperationMetadata empty() {
    return new ErdOperationMetadata(null, null, null, null);
  }

  public ErdOperationMetadata withSessionId(String sessionId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId);
  }

  public ErdOperationMetadata withClientOperationId(String clientOperationId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId);
  }

  public ErdOperationMetadata withBaseSchemaRevision(Long baseSchemaRevision) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId);
  }

  public ErdOperationMetadata withActorUserId(String actorUserId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId);
  }

  public String actorUserIdOr(String defaultValue) {
    return actorUserId != null ? actorUserId : defaultValue;
  }

}
