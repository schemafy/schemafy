package com.schemafy.core.erd.memo.application.port.in;

public record DeleteMemoCommentCommand(
    String commentId,
    String requesterId,
    boolean canDeleteOthers) {
}
