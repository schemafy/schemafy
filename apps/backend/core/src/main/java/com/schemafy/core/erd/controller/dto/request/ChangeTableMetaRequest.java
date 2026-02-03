package com.schemafy.core.erd.controller.dto.request;

public record ChangeTableMetaRequest(
    String charset,
    String collation) {
}
