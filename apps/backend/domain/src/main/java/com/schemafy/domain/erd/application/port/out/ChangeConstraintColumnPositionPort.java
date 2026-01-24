package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface ChangeConstraintColumnPositionPort {

  Mono<Void> changeConstraintColumnPositions(String constraintId, List<ConstraintColumn> columns);

}
