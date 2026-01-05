package com.schemafy.core.erd.controller.dto.response;

import java.util.Collections;
import java.util.List;

import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstraintResponse {

  private String id;
  private String tableId;
  private String name;
  private String kind;
  private List<ConstraintColumnResponse> columns;

  public static ConstraintResponse from(Constraint constraint) {
    return ConstraintResponse.builder()
        .id(constraint.getId())
        .tableId(constraint.getTableId())
        .name(constraint.getName())
        .kind(constraint.getKind())
        .columns(Collections.emptyList())
        .build();
  }

  public static ConstraintResponse from(Constraint constraint,
      List<ConstraintColumn> columns) {
    return ConstraintResponse.builder()
        .id(constraint.getId())
        .tableId(constraint.getTableId())
        .name(constraint.getName())
        .kind(constraint.getKind())
        .columns(columns.stream()
            .map(ConstraintColumnResponse::from)
            .toList())
        .build();
  }

}
