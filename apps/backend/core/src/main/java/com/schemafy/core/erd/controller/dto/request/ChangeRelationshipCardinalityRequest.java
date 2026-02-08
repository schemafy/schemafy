package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;

public record ChangeRelationshipCardinalityRequest(
    @NotNull(message = "cardinality는 필수입니다.") Cardinality cardinality) {
}
