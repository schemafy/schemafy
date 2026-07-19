package com.schemafy.core.erd.operation.application.inverse;

import com.schemafy.core.erd.index.domain.type.SortDirection;

public record ChangeIndexColumnSortDirectionInverse(
    String indexColumnId,
    SortDirection oldSortDirection) implements InversePayload {

}
