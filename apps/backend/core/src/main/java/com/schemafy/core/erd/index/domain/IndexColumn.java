package com.schemafy.core.erd.index.domain;

import com.schemafy.core.erd.index.domain.type.SortDirection;

public record IndexColumn(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
