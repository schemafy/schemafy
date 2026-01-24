package com.schemafy.domain.erd.relationship.application.port.in;

public record CreateRelationshipColumnCommand(
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
