package com.schemafy.domain.erd.relationship.domain;

public record RelationshipColumn(
    String id,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
