package com.schemafy.domain.erd.constraint.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

public record CreateConstraintCommand(
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr,
    List<CreateConstraintColumnCommand> columns) {
}
