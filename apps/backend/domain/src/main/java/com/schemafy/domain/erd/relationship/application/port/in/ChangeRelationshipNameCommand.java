package com.schemafy.domain.erd.relationship.application.port.in;

public record ChangeRelationshipNameCommand(
    String relationshipId,
    String newName) {
}
