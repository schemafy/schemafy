package com.schemafy.api.erd.controller.dto.response;

import java.util.List;

public record TableSnapshotResponse(
    TableResponse table,
    List<ColumnResponse> columns,
    List<ConstraintSnapshotResponse> constraints,
    List<RelationshipSnapshotResponse> relationships,
    List<IndexSnapshotResponse> indexes) {
}
