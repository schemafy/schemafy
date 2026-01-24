package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.SortDirection;

public record CreateIndexColumnCommand(
    String columnId,
    int seqNo,
    SortDirection sortDirection) {
}
