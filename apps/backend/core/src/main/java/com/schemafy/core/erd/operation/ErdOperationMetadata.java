package com.schemafy.core.erd.operation;

import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

public record ErdOperationMetadata(
    String sessionId,
    String clientOperationId,
    Long baseSchemaRevision,
    String actorUserId,
    ErdOperationDerivationKind derivationKind,
    String derivedFromOpId) {

  public static ErdOperationMetadata empty() {
    return new ErdOperationMetadata(null, null, null, null, null, null);
  }

  public ErdOperationMetadata(
      String sessionId,
      String clientOperationId,
      Long baseSchemaRevision,
      String actorUserId) {
    this(sessionId, clientOperationId, baseSchemaRevision, actorUserId, null, null);
  }

  public ErdOperationMetadata withSessionId(String sessionId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId,
        derivationKind, derivedFromOpId);
  }

  public ErdOperationMetadata withClientOperationId(String clientOperationId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId,
        derivationKind, derivedFromOpId);
  }

  public ErdOperationMetadata withBaseSchemaRevision(Long baseSchemaRevision) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId,
        derivationKind, derivedFromOpId);
  }

  public ErdOperationMetadata withActorUserId(String actorUserId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId,
        derivationKind, derivedFromOpId);
  }

  public ErdOperationMetadata withDerivation(
      ErdOperationDerivationKind derivationKind, String derivedFromOpId) {
    return new ErdOperationMetadata(sessionId, clientOperationId, baseSchemaRevision, actorUserId,
        derivationKind, derivedFromOpId);
  }

  public String actorUserIdOr(String defaultValue) {
    return actorUserId != null ? actorUserId : defaultValue;
  }

  public ErdOperationDerivationKind derivationKindOrDefault() {
    return derivationKind != null ? derivationKind : ErdOperationDerivationKind.ORIGINAL;
  }

}
