package com.schemafy.core.erd.constraint.domain;

public record ConstraintColumn(
    String id,
    String constraintId,
    String columnId,
    int seqNo) {

  public ConstraintColumn withSeqNo(int nextSeqNo) {
    return new ConstraintColumn(id, constraintId, columnId, nextSeqNo);
  }

}
