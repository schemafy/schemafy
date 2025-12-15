package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.ConstraintColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstraintColumnResponse {

    private String id;
    private String constraintId;
    private String columnId;
    private Integer seqNo;

    public static ConstraintColumnResponse from(
            ConstraintColumn constraintColumn) {
        return ConstraintColumnResponse.builder()
                .id(constraintColumn.getId())
                .constraintId(constraintColumn.getConstraintId())
                .columnId(constraintColumn.getColumnId())
                .seqNo(constraintColumn.getSeqNo())
                .build();
    }

}
