package com.schemafy.core.erd.controller.dto.request;

public record ChangeColumnMetaRequest(
    Boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
