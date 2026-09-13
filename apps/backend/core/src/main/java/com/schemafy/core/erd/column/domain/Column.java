package com.schemafy.core.erd.column.domain;

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

  public Column withSeqNo(int nextSeqNo) {
    return new Column(
        id,
        tableId,
        name,
        dataType,
        typeArguments,
        nextSeqNo,
        autoIncrement,
        charset,
        collation,
        comment);
  }

}
