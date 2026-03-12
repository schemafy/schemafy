package com.schemafy.core.erd.memo.application.port.in;

public record UpdateMemoPositionCommand(
    String memoId,
    String positions,
    String requesterId) {
}
