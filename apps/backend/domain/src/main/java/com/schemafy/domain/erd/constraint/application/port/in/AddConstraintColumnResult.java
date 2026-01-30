package com.schemafy.domain.erd.constraint.application.port.in;

import java.util.List;

public record AddConstraintColumnResult(
    String constraintColumnId,
    String constraintId,
    String columnId,
    int seqNo,
    List<CascadeCreatedColumn> cascadeCreatedColumns) {

  public record CascadeCreatedColumn(
      String fkColumnId,
      String fkColumnName,
      String fkTableId,
      String relationshipColumnId,
      String relationshipId) {}
}
