package com.schemafy.domain.erd.memo.domain;

import java.time.Instant;

public record MemoComment(
    String id,
    String memoId,
    String authorId,
    String body,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt) {

  public MemoComment withBody(String nextBody) {
    return new MemoComment(id, memoId, authorId, nextBody, createdAt, updatedAt,
        deletedAt);
  }

  public MemoComment softDeleted(Instant deletedAtAt) {
    return new MemoComment(id, memoId, authorId, body, createdAt, updatedAt,
        deletedAtAt);
  }

}
