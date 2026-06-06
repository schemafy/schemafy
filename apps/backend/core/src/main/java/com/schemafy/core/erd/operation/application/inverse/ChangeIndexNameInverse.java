package com.schemafy.core.erd.operation.application.inverse;

public record ChangeIndexNameInverse(
    String indexId,
    String oldName) implements InversePayload {

}
