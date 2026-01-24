package com.schemafy.domain.erd.relationship.application.port.in;

public record AddRelationshipColumnCommand(
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
