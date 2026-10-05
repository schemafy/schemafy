package com.schemafy.api.erd.controller.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.common.annotation.JsonObject;

public record ChangeRelationshipControlPointsRequest(
    @JsonObject JsonNode controlPoint1,
    @JsonObject(nullable = true) JsonNode controlPoint2) {
}
