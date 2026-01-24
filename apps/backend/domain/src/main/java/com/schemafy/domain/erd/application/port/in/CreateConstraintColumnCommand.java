package com.schemafy.domain.erd.application.port.in;

public record CreateConstraintColumnCommand(
    String columnId,
    int seqNo) {
}
