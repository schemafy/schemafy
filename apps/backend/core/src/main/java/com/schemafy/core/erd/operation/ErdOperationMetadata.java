package com.schemafy.core.erd.operation;

import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

public record ErdOperationMetadata(
    String sessionId,
    String clientOperationId,
    Long baseSchemaRevision,
    String actorUserId,
    ErdOperationDerivationKind derivationKind,
    String derivedFromOpId) {

  public ErdOperationMetadata(
      String sessionId,
      String clientOperationId,
      Long baseSchemaRevision,
      String actorUserId) {
    this(sessionId, clientOperationId, baseSchemaRevision, actorUserId, null, null);
  }

  public static ErdOperationMetadata empty() {
    return new ErdOperationMetadata(null, null, null, null, null, null);
  }

  public ErdOperationMetadata withSessionId(String sessionId) {
    return new ErdOperationMetadata(
        sessionId,
        clientOperationId,
        baseSchemaRevision,
        actorUserId,
        derivationKind,
        derivedFromOpId);
  }

  public ErdOperationMetadata withClientOperationId(String clientOperationId) {
    return new ErdOperationMetadata(
        sessionId,
        clientOperationId,
        baseSchemaRevision,
        actorUserId,
        derivationKind,
        derivedFromOpId);
  }

  public ErdOperationMetadata withBaseSchemaRevision(Long baseSchemaRevision) {
    return new ErdOperationMetadata(
        sessionId,
        clientOperationId,
        baseSchemaRevision,
        actorUserId,
        derivationKind,
        derivedFromOpId);
  }

  public ErdOperationMetadata withActorUserId(String actorUserId) {
    return new ErdOperationMetadata(
        sessionId,
        clientOperationId,
        baseSchemaRevision,
        actorUserId,
        derivationKind,
        derivedFromOpId);
  }

  public ErdOperationMetadata withDerivationKind(ErdOperationDerivationKind derivationKind) {
    return new ErdOperationMetadata(
        sessionId,
        clientOperationId,
        baseSchemaRevision,
        actorUserId,
        derivationKind,
        derivedFromOpId);
  }

  public ErdOperationMetadata withDerivedFromOpId(String derivedFromOpId) {
    return new ErdOperationMetadata(
        sessionId,
        clientOperationId,
        baseSchemaRevision,
        actorUserId,
        derivationKind,
        derivedFromOpId);
  }

  public String actorUserIdOr(String defaultValue) {
    return actorUserId != null ? actorUserId : defaultValue;
  }

  public ErdOperationDerivationKind derivationKindOr(ErdOperationDerivationKind defaultValue) {
    return derivationKind != null ? derivationKind : defaultValue;
  }

}
