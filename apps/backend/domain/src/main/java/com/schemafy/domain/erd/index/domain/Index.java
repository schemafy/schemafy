package com.schemafy.domain.erd.index.domain;

import com.schemafy.domain.erd.index.domain.type.IndexType;

public record Index(
    String id,
    String tableId,
    String name,
    IndexType type) {
}
