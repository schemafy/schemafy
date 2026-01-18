package com.schemafy.domain.erd.domain;

public record ConstraintColumn(
    String id,
    String constraintId,
    String columnId,
    int seqNo) {
}
