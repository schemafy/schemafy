package com.schemafy.domain.erd.application.port.in;

public record CreateRelationshipColumnCommand(
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
