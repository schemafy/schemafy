package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.table.application.port.in.CreateTableResult;
import com.schemafy.core.erd.table.domain.Table;

public record TableResponse(
    String id,
    String schemaId,
    String name,
    String charset,
    String collation,
    JsonNode extra) {

  public static TableResponse from(CreateTableResult result, String schemaId) {
    return new TableResponse(
        result.tableId(),
        schemaId,
        result.name(),
        result.charset(),
        result.collation(),
        JsonCodec.parseOptionalNode(result.extra()));
  }

  public static TableResponse from(Table table) {
    return new TableResponse(
        table.id(),
        table.schemaId(),
        table.name(),
        table.charset(),
        table.collation(),
        JsonCodec.parseOptionalNode(table.extra()));
  }

}
