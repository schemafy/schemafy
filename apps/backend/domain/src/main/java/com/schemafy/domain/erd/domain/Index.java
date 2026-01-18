package com.schemafy.domain.erd.domain;

import com.schemafy.domain.erd.domain.type.IndexType;

public record Index(
    String id,
    String tableId,
    String name,
    IndexType type) {
}
