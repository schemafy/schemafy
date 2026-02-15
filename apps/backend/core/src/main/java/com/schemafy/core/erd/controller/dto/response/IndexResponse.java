package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.index.application.port.in.CreateIndexResult;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.type.IndexType;

public record IndexResponse(
    String id,
    String tableId,
    String name,
    IndexType type) {

  public static IndexResponse from(CreateIndexResult result, String tableId) {
    return new IndexResponse(
        result.indexId(),
        tableId,
        result.name(),
        result.type());
  }

  public static IndexResponse from(Index index) {
    return new IndexResponse(
        index.id(),
        index.tableId(),
        index.name(),
        index.type());
  }

}
