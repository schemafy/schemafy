package com.schemafy.core.erd.table.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.table.domain.Table;

@Component
class TableMapper {

  private final JsonCodec jsonCodec;

  TableMapper(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

  TableEntity toEntity(Table table) {
    return new TableEntity(
        table.id(),
        table.schemaId(),
        table.name(),
        table.charset(),
        table.collation(),
        table.extra());
  }

  Table toDomain(TableEntity entity) {
    return new Table(
        entity.getId(),
        entity.getSchemaId(),
        entity.getName(),
        entity.getCharset(),
        entity.getCollation(),
        jsonCodec.normalizePersistedJson(entity.getExtra()));
  }

}
