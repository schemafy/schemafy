package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record ChangeRelationshipKindCommand(
    String relationshipId,
    RelationshipKind kind) {
}
