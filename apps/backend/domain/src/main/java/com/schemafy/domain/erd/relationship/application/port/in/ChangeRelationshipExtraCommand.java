package com.schemafy.domain.erd.relationship.application.port.in;

public record ChangeRelationshipExtraCommand(
    String relationshipId,
    String extra) {
}
