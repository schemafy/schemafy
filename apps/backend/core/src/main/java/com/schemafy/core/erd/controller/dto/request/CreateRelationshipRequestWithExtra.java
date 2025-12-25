package com.schemafy.core.erd.controller.dto.request;

import validation.Validation.CreateRelationshipRequest;

public record CreateRelationshipRequestWithExtra(
    CreateRelationshipRequest request,
    String extra) {
}
