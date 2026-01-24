package com.schemafy.domain.erd.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.domain.type.IndexType;

public record CreateIndexCommand(
    String tableId,
    String name,
    IndexType type,
    List<CreateIndexColumnCommand> columns) {
}
