package com.schemafy.core.erd.schema.application.port.in;

import com.schemafy.core.erd.schema.domain.Schema;

public record GetSchemaWithRevisionResult(
    Schema schema,
    long currentRevision) {
}
