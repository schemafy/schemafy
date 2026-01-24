package com.schemafy.domain.erd.application.port.in;

public record AddConstraintColumnResult(
    String constraintColumnId,
    String constraintId,
    String columnId,
    int seqNo) {
}
