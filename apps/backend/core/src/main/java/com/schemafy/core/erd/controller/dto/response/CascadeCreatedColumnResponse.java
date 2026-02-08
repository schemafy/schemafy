package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnResult;

public record CascadeCreatedColumnResponse(
    String fkColumnId,
    String fkColumnName,
    String fkTableId,
    String relationshipColumnId,
    String relationshipId,
    String pkConstraintColumnId,
    String pkConstraintId) {

  public static CascadeCreatedColumnResponse from(
      AddConstraintColumnResult.CascadeCreatedColumn column) {
    return new CascadeCreatedColumnResponse(
        column.fkColumnId(),
        column.fkColumnName(),
        column.fkTableId(),
        column.relationshipColumnId(),
        column.relationshipId(),
        column.pkConstraintColumnId(),
        column.pkConstraintId());
  }

}
