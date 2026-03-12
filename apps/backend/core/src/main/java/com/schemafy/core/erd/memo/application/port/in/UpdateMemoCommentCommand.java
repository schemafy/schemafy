package com.schemafy.core.erd.memo.application.port.in;

public record UpdateMemoCommentCommand(
    String commentId,
    String body,
    String requesterId) {
}
