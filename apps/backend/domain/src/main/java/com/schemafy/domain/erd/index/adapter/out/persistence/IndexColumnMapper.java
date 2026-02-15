package com.schemafy.domain.erd.index.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.SortDirection;

@Component
class IndexColumnMapper {

  IndexColumnEntity toEntity(IndexColumn indexColumn) {
    return new IndexColumnEntity(
        indexColumn.id(),
        indexColumn.indexId(),
        indexColumn.columnId(),
        indexColumn.seqNo(),
        indexColumn.sortDirection().name());
  }

  IndexColumn toDomain(IndexColumnEntity entity) {
    return new IndexColumn(
        entity.getId(),
        entity.getIndexId(),
        entity.getColumnId(),
        entity.getSeqNo(),
        SortDirection.valueOf(entity.getSortDirection()));
  }

}
