package com.schemafy.core.erd.relationship.domain;

public record RelationshipColumn(
    String id,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {
}
