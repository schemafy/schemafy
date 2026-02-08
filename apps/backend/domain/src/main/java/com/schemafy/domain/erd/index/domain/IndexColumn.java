package com.schemafy.domain.erd.index.domain;

import com.schemafy.domain.erd.index.domain.type.SortDirection;

public record IndexColumn(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
