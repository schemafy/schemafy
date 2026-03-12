package com.schemafy.core.erd.relationship.application.port.in;

public record ChangeRelationshipExtraCommand(
    String relationshipId,
    String extra) {
}
