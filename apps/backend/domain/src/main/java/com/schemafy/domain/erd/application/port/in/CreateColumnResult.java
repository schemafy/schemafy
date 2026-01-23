package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.ColumnLengthScale;

public record CreateColumnResult(
    String columnId,
    String name,
    String dataType,
    ColumnLengthScale lengthScale,
    int seqNo,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
