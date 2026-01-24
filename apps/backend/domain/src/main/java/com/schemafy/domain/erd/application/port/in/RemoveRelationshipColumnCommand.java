package com.schemafy.domain.erd.application.port.in;

public record RemoveRelationshipColumnCommand(
    String relationshipId,
    String relationshipColumnId) {
}
