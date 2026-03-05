package com.schemafy.domain.erd.column.domain;

public record Column(
    String id,
    String tableId,
    String name,
    String dataType,
    ColumnTypeArguments typeArguments,
    int seqNo,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
