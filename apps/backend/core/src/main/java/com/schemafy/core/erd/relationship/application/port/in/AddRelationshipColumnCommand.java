package com.schemafy.core.erd.relationship.application.port.in;

public record AddRelationshipColumnCommand(
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    Integer seqNo) {
}
