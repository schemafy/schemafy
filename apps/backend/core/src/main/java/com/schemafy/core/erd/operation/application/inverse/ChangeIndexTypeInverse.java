package com.schemafy.core.erd.operation.application.inverse;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record ChangeIndexTypeInverse(
    String indexId,
    IndexType oldType) implements InversePayload {

}
