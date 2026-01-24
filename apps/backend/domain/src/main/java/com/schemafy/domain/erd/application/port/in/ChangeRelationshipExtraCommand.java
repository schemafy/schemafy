package com.schemafy.domain.erd.application.port.in;

public record ChangeRelationshipExtraCommand(
    String relationshipId,
    String extra) {
}
