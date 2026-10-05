package com.schemafy.core.erd.relationship.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record ChangeRelationshipControlPointsCommand(
    String relationshipId,
    JsonNode controlPoint1,
    JsonNode controlPoint2) {
}
