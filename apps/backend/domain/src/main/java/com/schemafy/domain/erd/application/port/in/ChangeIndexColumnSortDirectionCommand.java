package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.SortDirection;

public record ChangeIndexColumnSortDirectionCommand(
    String indexColumnId,
    SortDirection sortDirection) {
}
