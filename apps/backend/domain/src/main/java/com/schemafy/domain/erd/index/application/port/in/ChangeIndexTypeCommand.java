package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.erd.index.domain.type.IndexType;

public record ChangeIndexTypeCommand(
    String indexId,
    IndexType type) {
}
