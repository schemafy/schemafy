package com.schemafy.core.erd.memo.application.port.in;

public record CreateMemoCommentCommand(
    String memoId,
    String body,
    String authorId) {
}
