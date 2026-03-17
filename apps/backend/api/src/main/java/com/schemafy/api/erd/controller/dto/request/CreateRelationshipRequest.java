package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.common.annotation.JsonObject;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record CreateRelationshipRequest(
    @NotBlank(message = "fkTableId는 필수입니다.") String fkTableId,
    @NotBlank(message = "pkTableId는 필수입니다.") String pkTableId,
    @NotNull(message = "kind는 필수입니다.") RelationshipKind kind,
    @NotNull(message = "cardinality는 필수입니다.") Cardinality cardinality,
    @JsonObject(nullable = true)
    JsonNode extra) {
}
