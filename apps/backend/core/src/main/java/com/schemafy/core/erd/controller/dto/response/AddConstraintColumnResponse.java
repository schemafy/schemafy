package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnResult;

public record AddConstraintColumnResponse(
    String id,
    String constraintId,
    String columnId,
    int seqNo,
    List<CascadeCreatedColumnResponse> cascadeCreatedColumns) {

  public static AddConstraintColumnResponse from(AddConstraintColumnResult result) {
    List<CascadeCreatedColumnResponse> cascades = result.cascadeCreatedColumns().stream()
        .map(CascadeCreatedColumnResponse::from)
        .toList();
    return new AddConstraintColumnResponse(
        result.constraintColumnId(),
        result.constraintId(),
        result.columnId(),
        result.seqNo(),
        cascades);
  }

}
