package com.schemafy.core.erd.operation.application.inverse;

public record ChangeRelationshipNameInverse(
    String relationshipId,
    String oldName) implements InversePayload {

}
