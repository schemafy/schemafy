package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public record ChangeRelationshipKindRequest(
    @NotNull(message = "kind는 필수입니다.") RelationshipKind kind) {
}
