package com.schemafy.domain.erd.relationship.application.port.in;

public record AddRelationshipColumnResult(
    String relationshipColumnId,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
