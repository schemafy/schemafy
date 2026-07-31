package com.schemafy.api.erd.service.table;

import org.springframework.stereotype.Component;

import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.table.application.port.in.CreateTableResult;
import com.schemafy.core.erd.table.domain.Table;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TableApiResponseMapper {

  private final JsonObjectMetadataConverter jsonObjectMetadataConverter;

  public TableResponse toTableResponse(CreateTableResult result,
      String schemaId) {
    return new TableResponse(
        result.tableId(),
        schemaId,
        result.name(),
        result.charset(),
        result.collation(),
        jsonObjectMetadataConverter.toOptionalJsonNode(result.extra()));
  }

  public TableResponse toTableResponse(Table table) {
    return new TableResponse(
        table.id(),
        table.schemaId(),
        table.name(),
        table.charset(),
        table.collation(),
        jsonObjectMetadataConverter.toOptionalJsonNode(table.extra()));
  }

}
