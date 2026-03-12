package com.schemafy.core.erd.relationship.application.port.in;

public record ChangeRelationshipColumnPositionCommand(
    String relationshipColumnId,
    int seqNo) {
}
