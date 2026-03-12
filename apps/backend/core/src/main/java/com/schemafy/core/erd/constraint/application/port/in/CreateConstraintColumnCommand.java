package com.schemafy.core.erd.constraint.application.port.in;

public record CreateConstraintColumnCommand(
    String columnId,
    Integer seqNo) {
}
