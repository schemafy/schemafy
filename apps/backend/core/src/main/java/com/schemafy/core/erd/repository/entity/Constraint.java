package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

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
@Table("db_constraints")
public class Constraint extends BaseEntity {

    @Column("table_id")
    private String tableId;

    @Column("name")
    private String name;

    @Column("kind")
    private String kind;

    @Nullable
    @Column("check_expr")
    private String checkExpr;

    @Nullable
    @Column("default_expr")
    private String defaultExpr;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static Constraint newConstraint(String tableId, String name,
            String kind,
            String checkExpr, String defaultExpr) {
        Constraint constraint = new Constraint(tableId, name, kind, checkExpr,
                defaultExpr);
        constraint.setId(UlidGenerator.generate());
        return constraint;
    }

}
