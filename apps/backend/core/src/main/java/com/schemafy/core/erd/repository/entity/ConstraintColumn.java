package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_constraint_columns")
public class ConstraintColumn extends BaseEntity {

    @Column("constraint_id")
    private String constraintId;

    @Column("column_id")
    private String columnId;

    @Column("seq_no")
    private int seqNo;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static ConstraintColumn newConstraintColumn(String constraintId,
            String columnId, int seqNo) {
        ConstraintColumn constraintColumn = new ConstraintColumn(constraintId,
                columnId, seqNo);
        constraintColumn.setId(UlidGenerator.generate());
        return constraintColumn;
    }

}
