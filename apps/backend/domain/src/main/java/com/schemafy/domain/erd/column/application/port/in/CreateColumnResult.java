package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.erd.column.domain.ColumnTypeArguments;

public record CreateColumnResult(
    String columnId,
    String name,
    String dataType,
    ColumnTypeArguments typeArguments,
    int seqNo,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
