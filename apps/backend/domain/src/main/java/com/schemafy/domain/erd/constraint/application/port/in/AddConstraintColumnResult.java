package com.schemafy.domain.erd.constraint.application.port.in;

public record AddConstraintColumnResult(
    String constraintColumnId,
    String constraintId,
    String columnId,
    int seqNo) {
}
