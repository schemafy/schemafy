package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

public record RelationshipSnapshotResponse(
    RelationshipResponse relationship,
    List<RelationshipColumnResponse> columns) {
}
