package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

@Component
class ConstraintMapper {

  ConstraintEntity toEntity(Constraint constraint) {
    return new ConstraintEntity(
        constraint.id(),
        constraint.tableId(),
        constraint.name(),
        constraint.kind().name(),
        constraint.checkExpr(),
        constraint.defaultExpr());
  }

  Constraint toDomain(ConstraintEntity entity) {
    return new Constraint(
        entity.getId(),
        entity.getTableId(),
        entity.getName(),
        ConstraintKind.valueOf(entity.getKind()),
        entity.getCheckExpr(),
        entity.getDefaultExpr());
  }

}
