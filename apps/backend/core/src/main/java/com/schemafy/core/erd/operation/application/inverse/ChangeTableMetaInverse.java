package com.schemafy.core.erd.operation.application.inverse;

public record ChangeTableMetaInverse(
    String tableId,
    String oldCharset,
    String oldCollation) implements InversePayload {

}
