package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

public record IndexSnapshotResponse(
    IndexResponse index,
    List<IndexColumnResponse> columns) {
}
