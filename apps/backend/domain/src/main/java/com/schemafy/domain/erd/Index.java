package com.schemafy.domain.erd;

import com.schemafy.domain.erd.type.IndexType;

public record Index(
    String id,
    String tableId,
    String name,
    IndexType type) {
}
