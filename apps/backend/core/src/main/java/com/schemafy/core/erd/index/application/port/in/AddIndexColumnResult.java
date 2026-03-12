package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.erd.index.domain.type.SortDirection;

public record AddIndexColumnResult(
    String indexColumnId,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
