package com.schemafy.core.erd.index.application.port.in;

import java.util.List;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record CreateIndexCommand(
    String tableId,
    String name,
    IndexType type,
    List<CreateIndexColumnCommand> columns) {
}
