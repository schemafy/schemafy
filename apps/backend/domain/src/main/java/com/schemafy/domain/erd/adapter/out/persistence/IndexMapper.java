package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.domain.Index;
import com.schemafy.domain.erd.domain.type.IndexType;

@Component
class IndexMapper {

  IndexEntity toEntity(Index index) {
    return new IndexEntity(
        index.id(),
        index.tableId(),
        index.name(),
        index.type().name());
  }

  Index toDomain(IndexEntity entity) {
    return new Index(
        entity.getId(),
        entity.getTableId(),
        entity.getName(),
        IndexType.valueOf(entity.getType()));
  }
}
