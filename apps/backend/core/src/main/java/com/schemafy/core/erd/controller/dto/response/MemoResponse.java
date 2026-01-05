package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.erd.repository.entity.Memo;
import com.schemafy.core.user.controller.dto.response.UserSummaryResponse;

public record MemoResponse(
    String id,
    String schemaId,
    UserSummaryResponse author,
    String positions,
    Instant createdAt,
    Instant updatedAt) {

  public static MemoResponse from(Memo memo, UserSummaryResponse author) {
    return new MemoResponse(
        memo.getId(),
        memo.getSchemaId(),
        author,
        memo.getPositions(),
        memo.getCreatedAt(),
        memo.getUpdatedAt());
  }

}
