package com.schemafy.domain.erd.relationship.application.port.in;

public record ChangeRelationshipColumnPositionCommand(
    String relationshipColumnId,
    int seqNo) {
}
