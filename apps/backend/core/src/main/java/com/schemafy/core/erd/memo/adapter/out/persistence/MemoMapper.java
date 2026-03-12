package com.schemafy.core.erd.memo.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.memo.domain.Memo;

@Component
class MemoMapper {

  MemoEntity toEntity(Memo memo) {
    return new MemoEntity(
        memo.id(),
        memo.schemaId(),
        memo.authorId(),
        memo.positions(),
        memo.createdAt(),
        memo.updatedAt(),
        memo.deletedAt());
  }

  Memo toDomain(MemoEntity entity) {
    return new Memo(
        entity.getId(),
        entity.getSchemaId(),
        entity.getAuthorId(),
        entity.getPositions(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt());
  }

}
