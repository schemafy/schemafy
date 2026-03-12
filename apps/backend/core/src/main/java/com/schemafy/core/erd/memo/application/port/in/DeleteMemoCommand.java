package com.schemafy.core.erd.memo.application.port.in;

public record DeleteMemoCommand(
    String memoId,
    String requesterId,
    boolean canDeleteOthers) {
}
