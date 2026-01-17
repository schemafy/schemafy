package com.schemafy.domain.erd;

public record ConstraintColumn(
    String id,
    String constraintId,
    String columnId,
    int seqNo) {
}
