package com.schemafy.domain.erd.constraint.domain;

public record ConstraintColumn(
    String id,
    String constraintId,
    String columnId,
    int seqNo) {
}
