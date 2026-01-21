package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.domain.Table;

@Component
class TableMapper {

  TableEntity toEntity(Table table) {
    return TableEntity.builder()
        .id(table.id())
        .schemaId(table.schemaId())
        .name(table.name())
        .charset(table.charset())
        .collation(table.collation())
        .build();
  }

  Table toDomain(TableEntity entity) {
    return new Table(
        entity.getId(),
        entity.getSchemaId(),
        entity.getName(),
        entity.getCharset(),
        entity.getCollation());
  }

}
