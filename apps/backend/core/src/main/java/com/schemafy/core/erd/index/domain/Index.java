package com.schemafy.core.erd.index.domain;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record Index(
    String id,
    String tableId,
    String name,
    IndexType type) {
}
