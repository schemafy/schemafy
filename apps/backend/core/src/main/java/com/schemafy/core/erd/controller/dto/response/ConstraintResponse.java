package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.Constraint;

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

    public static ConstraintResponse from(Constraint constraint) {
        return ConstraintResponse.builder()
                .id(constraint.getId())
                .tableId(constraint.getTableId())
                .name(constraint.getName())
                .kind(constraint.getKind())
                .build();
    }
}
