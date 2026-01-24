package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.SortDirection;

public record AddIndexColumnResult(
    String indexColumnId,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
