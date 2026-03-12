package com.schemafy.core.erd.constraint.application.port.out;

import java.util.List;

import com.schemafy.core.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface ChangeConstraintColumnPositionPort {

  Mono<Void> changeConstraintColumnPositions(String constraintId, List<ConstraintColumn> columns);

}
