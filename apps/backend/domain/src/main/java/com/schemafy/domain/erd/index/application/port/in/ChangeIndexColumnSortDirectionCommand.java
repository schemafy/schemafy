package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.erd.index.domain.type.SortDirection;

public record ChangeIndexColumnSortDirectionCommand(
    String indexColumnId,
    SortDirection sortDirection) {
}
