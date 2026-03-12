package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.erd.index.domain.type.SortDirection;

public record ChangeIndexColumnSortDirectionCommand(
    String indexColumnId,
    SortDirection sortDirection) {
}
