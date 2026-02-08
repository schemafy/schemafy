package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

public record TableSnapshotResponse(
    TableResponse table,
    List<ColumnResponse> columns,
    List<ConstraintSnapshotResponse> constraints,
    List<RelationshipSnapshotResponse> relationships) {
}
