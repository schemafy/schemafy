package com.schemafy.core.erd.relationship.application.port.in;

public record ChangeRelationshipNameCommand(
    String relationshipId,
    String newName) {
}
