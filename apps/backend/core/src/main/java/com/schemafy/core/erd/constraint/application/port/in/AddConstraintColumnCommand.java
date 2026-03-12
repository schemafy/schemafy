package com.schemafy.core.erd.constraint.application.port.in;

public record AddConstraintColumnCommand(
    String constraintId,
    String columnId,
    Integer seqNo) {
}
