package com.schemafy.domain.erd;

public record Column(
    String id,
    String tableId,
    String name,
    String dataType,
    Integer length,
    Integer precision,
    Integer scale,
    int seqNo,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
