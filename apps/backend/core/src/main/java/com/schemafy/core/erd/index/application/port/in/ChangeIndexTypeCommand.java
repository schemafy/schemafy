package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record ChangeIndexTypeCommand(
    String indexId,
    IndexType type) {
}
