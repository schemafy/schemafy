package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record CreateIndexResult(
    String indexId,
    String name,
    IndexType type) {
}
