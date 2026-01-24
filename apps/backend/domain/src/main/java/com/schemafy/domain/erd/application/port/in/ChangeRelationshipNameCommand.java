package com.schemafy.domain.erd.application.port.in;

public record ChangeRelationshipNameCommand(
    String relationshipId,
    String newName) {
}
