package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.ColumnLengthScale;

@Component
class ColumnMapper {

  ColumnEntity toEntity(Column column) {
    return new ColumnEntity(
        column.id(),
        column.tableId(),
        column.name(),
        column.dataType(),
        toLengthScaleJson(column.lengthScale()),
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
        ColumnLengthScale.fromJson(entity.getLengthScale()),
        entity.getSeqNo(),
        Boolean.TRUE.equals(entity.getAutoIncrement()),
        entity.getCharset(),
        entity.getCollation(),
        entity.getComment());
  }

  String toLengthScaleJson(ColumnLengthScale lengthScale) {
    if (lengthScale == null) {
      return null;
    }
    return lengthScale.toJson();
  }
}
