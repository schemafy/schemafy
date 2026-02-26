package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public record CreateRelationshipRequest(
    @NotBlank(message = "fkTableId는 필수입니다.") String fkTableId,
    @NotBlank(message = "pkTableId는 필수입니다.") String pkTableId,
    @NotNull(message = "kind는 필수입니다.") RelationshipKind kind,
    @NotNull(message = "cardinality는 필수입니다.") Cardinality cardinality,
    @JsonDeserialize(using = JsonValueToStringDeserializer.class) String extra) {
}
