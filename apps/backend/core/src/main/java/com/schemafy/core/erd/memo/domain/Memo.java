package com.schemafy.core.erd.memo.domain;

import java.time.Instant;

public record Memo(
    String id,
    String schemaId,
    String authorId,
    String positions,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt) {

  public Memo withPositions(String nextPositions) {
    return new Memo(id, schemaId, authorId, nextPositions, createdAt, updatedAt,
        deletedAt);
  }

  public Memo softDeleted(Instant deletedAtAt) {
    return new Memo(id, schemaId, authorId, positions, createdAt, updatedAt,
        deletedAtAt);
  }

}
