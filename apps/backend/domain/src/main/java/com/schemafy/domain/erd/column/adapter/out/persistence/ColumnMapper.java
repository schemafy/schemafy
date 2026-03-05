package com.schemafy.domain.erd.column.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnTypeArguments;

@Component
class ColumnMapper {

  ColumnEntity toEntity(Column column) {
    return new ColumnEntity(
        column.id(),
        column.tableId(),
        column.name(),
        column.dataType(),
        toTypeArgumentsJson(column.typeArguments()),
        column.seqNo(),
        column.autoIncrement(),
        column.charset(),
        column.collation(),
        column.comment());
  }

  Column toDomain(ColumnEntity entity) {
    return new Column(
        entity.getId(),
        entity.getTableId(),
        entity.getName(),
        entity.getDataType(),
        ColumnTypeArguments.fromJson(entity.getTypeArguments()),
        entity.getSeqNo(),
        Boolean.TRUE.equals(entity.getAutoIncrement()),
        entity.getCharset(),
        entity.getCollation(),
        entity.getComment());
  }

  String toTypeArgumentsJson(ColumnTypeArguments typeArguments) {
    if (typeArguments == null) {
      return null;
    }
    return typeArguments.toJson();
  }

}
