package com.schemafy.domain.erd.memo.application.port.in;

public record UpdateMemoPositionCommand(
    String memoId,
    String positions,
    String requesterId) {
}
