package com.schemafy.api.erd.controller.dto.response;

import java.util.List;

public record ConstraintSnapshotResponse(
    ConstraintResponse constraint,
    List<ConstraintColumnResponse> columns) {
}
