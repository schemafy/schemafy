package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

@Component
class ConstraintColumnMapper {

  ConstraintColumnEntity toEntity(ConstraintColumn constraintColumn) {
    return new ConstraintColumnEntity(
        constraintColumn.id(),
        constraintColumn.constraintId(),
        constraintColumn.columnId(),
        constraintColumn.seqNo());
  }

  ConstraintColumn toDomain(ConstraintColumnEntity entity) {
    return new ConstraintColumn(
        entity.getId(),
        entity.getConstraintId(),
        entity.getColumnId(),
        entity.getSeqNo());
  }

}
