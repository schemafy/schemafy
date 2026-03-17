package com.schemafy.core.erd.table.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.table.domain.Table;

@Component
class TableMapper {

  TableEntity toEntity(Table table) {
    return TableEntity.builder()
        .id(table.id())
        .schemaId(table.schemaId())
        .name(table.name())
        .charset(table.charset())
        .collation(table.collation())
        .extra(table.extra())
        .build();
  }

  Table toDomain(TableEntity entity) {
    return new Table(
        entity.getId(),
        entity.getSchemaId(),
        entity.getName(),
        entity.getCharset(),
        entity.getCollation(),
        JsonCodec.normalizePersistedJson(entity.getExtra()));
  }

}
