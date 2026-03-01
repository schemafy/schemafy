package com.schemafy.domain.erd.memo.application.port.in;

public record UpdateMemoCommentCommand(
    String memoId,
    String commentId,
    String body,
    String requesterId) {
}
