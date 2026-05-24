package com.schemafy.core.erd.operation.application.inverse;

public record ChangeColumnNameInverse(
    String columnId,
    String oldName) implements InversePayload {

}
