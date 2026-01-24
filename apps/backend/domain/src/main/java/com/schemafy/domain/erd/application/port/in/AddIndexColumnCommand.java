package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.SortDirection;

public record AddIndexColumnCommand(
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
