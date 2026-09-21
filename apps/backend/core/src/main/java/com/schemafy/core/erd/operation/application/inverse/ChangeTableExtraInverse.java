package com.schemafy.core.erd.operation.application.inverse;

public record ChangeTableExtraInverse(
    String tableId,
    String oldExtra) implements InversePayload {

}
