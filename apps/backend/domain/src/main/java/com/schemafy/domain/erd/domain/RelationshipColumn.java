package com.schemafy.domain.erd.domain;

public record RelationshipColumn(
    String id,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
