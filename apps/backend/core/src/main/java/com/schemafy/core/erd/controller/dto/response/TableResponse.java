package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.table.application.port.in.CreateTableResult;
import com.schemafy.domain.erd.table.domain.Table;

public record TableResponse(
    String id,
    String schemaId,
    String name,
    String charset,
    String collation,
    String extra) {

  public static TableResponse from(CreateTableResult result, String schemaId) {
    return new TableResponse(
        result.tableId(),
        schemaId,
        result.name(),
        result.charset(),
        result.collation(),
        null);
  }

  public static TableResponse from(Table table) {
    return new TableResponse(
        table.id(),
        table.schemaId(),
        table.name(),
        table.charset(),
        table.collation(),
        table.extra());
  }

}
