package com.schemafy.domain.erd.memo.application.port.in;

public record CreateMemoCommentCommand(
    String memoId,
    String body,
    String authorId) {
}
