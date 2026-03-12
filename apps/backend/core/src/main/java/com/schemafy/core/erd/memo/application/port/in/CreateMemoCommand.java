package com.schemafy.core.erd.memo.application.port.in;

public record CreateMemoCommand(
    String schemaId,
    String positions,
    String body,
    String authorId) {
}
