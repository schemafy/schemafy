package com.schemafy.domain.erd.application.port.in;

public record AddRelationshipColumnCommand(
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
