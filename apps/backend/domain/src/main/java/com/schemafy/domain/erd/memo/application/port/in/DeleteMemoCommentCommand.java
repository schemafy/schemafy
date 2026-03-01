package com.schemafy.domain.erd.memo.application.port.in;

public record DeleteMemoCommentCommand(
    String memoId,
    String commentId,
    String requesterId,
    boolean canDeleteOthers) {
}
