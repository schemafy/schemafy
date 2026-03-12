package com.schemafy.api.erd.controller.dto.response;

import java.util.List;

public record RelationshipSnapshotResponse(
    RelationshipResponse relationship,
    List<RelationshipColumnResponse> columns) {
}
