package com.schemafy.core.erd.relationship.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record ChangeRelationshipExtraCommand(
    String relationshipId,
    JsonNode extra) {
}
