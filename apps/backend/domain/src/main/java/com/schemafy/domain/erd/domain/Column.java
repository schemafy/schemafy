package com.schemafy.domain.erd.domain;

public record Column(
    String id,
    String tableId,
    String name,
    String dataType,
    ColumnLengthScale lengthScale,
    int seqNo,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
