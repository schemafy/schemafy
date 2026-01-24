package com.schemafy.domain.erd.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.domain.type.ConstraintKind;

public record CreateConstraintCommand(
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr,
    List<CreateConstraintColumnCommand> columns) {
}
