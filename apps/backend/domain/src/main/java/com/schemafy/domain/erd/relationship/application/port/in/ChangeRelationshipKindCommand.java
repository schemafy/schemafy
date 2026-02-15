package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public record ChangeRelationshipKindCommand(
    String relationshipId,
    RelationshipKind kind) {
}
