package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.column.application.port.in.CreateColumnResult;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;

public record ColumnResponse(
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

  public static ColumnResponse from(CreateColumnResult result, String tableId) {
    return new ColumnResponse(
        result.columnId(),
        tableId,
        result.name(),
        result.dataType(),
        result.lengthScale(),
        result.seqNo(),
        result.autoIncrement(),
        result.charset(),
        result.collation(),
        result.comment());
  }

  public static ColumnResponse from(Column column) {
    return new ColumnResponse(
        column.id(),
        column.tableId(),
        column.name(),
        column.dataType(),
        column.lengthScale(),
        column.seqNo(),
        column.autoIncrement(),
        column.charset(),
        column.collation(),
        column.comment());
  }

}
