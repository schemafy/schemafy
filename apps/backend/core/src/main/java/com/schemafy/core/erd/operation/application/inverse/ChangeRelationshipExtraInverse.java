package com.schemafy.core.erd.operation.application.inverse;

public record ChangeRelationshipExtraInverse(
    String relationshipId,
    String oldExtra) implements InversePayload {

}
