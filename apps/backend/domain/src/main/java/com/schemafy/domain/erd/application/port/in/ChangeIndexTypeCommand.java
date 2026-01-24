package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.IndexType;

public record ChangeIndexTypeCommand(
    String indexId,
    IndexType type) {
}
