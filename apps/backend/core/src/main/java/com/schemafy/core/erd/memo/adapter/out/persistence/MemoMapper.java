package com.schemafy.core.erd.memo.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.memo.domain.Memo;

@Component
class MemoMapper {

  private final JsonCodec jsonCodec;

  MemoMapper(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

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
        jsonCodec.normalizePersistedJson(entity.getPositions()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt());
  }

}
