package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.RelationshipKind;

public record ChangeRelationshipKindCommand(
    String relationshipId,
    RelationshipKind kind) {
}
