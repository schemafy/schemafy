package com.schemafy.domain.erd.domain;

import com.schemafy.domain.erd.domain.type.SortDirection;

public record IndexColumn(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
