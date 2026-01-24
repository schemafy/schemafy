package com.schemafy.domain.erd.application.port.in;

public record ChangeRelationshipColumnPositionCommand(
    String relationshipColumnId,
    int seqNo) {
}
