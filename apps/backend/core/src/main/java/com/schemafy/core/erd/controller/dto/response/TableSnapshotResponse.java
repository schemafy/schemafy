package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

public record TableSnapshotResponse(
    TableResponse table,
    List<ColumnResponse> columns,
    List<ConstraintSnapshotResponse> constraints,
    List<IndexSnapshotResponse> indexes, // TODO: 이거 제거
    List<RelationshipSnapshotResponse> relationships) {
}

