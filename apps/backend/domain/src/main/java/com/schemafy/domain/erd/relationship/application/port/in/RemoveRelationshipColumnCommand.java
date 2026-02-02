package com.schemafy.domain.erd.relationship.application.port.in;

public record RemoveRelationshipColumnCommand(
    String relationshipId,
    String relationshipColumnId) {
}
