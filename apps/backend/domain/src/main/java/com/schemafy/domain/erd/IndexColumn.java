package com.schemafy.domain.erd;

import com.schemafy.domain.erd.type.SortDirection;

public record IndexColumn(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
