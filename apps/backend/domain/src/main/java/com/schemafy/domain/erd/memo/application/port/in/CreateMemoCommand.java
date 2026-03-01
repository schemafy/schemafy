package com.schemafy.domain.erd.memo.application.port.in;

public record CreateMemoCommand(
    String schemaId,
    String positions,
    String body,
    String authorId) {
}
