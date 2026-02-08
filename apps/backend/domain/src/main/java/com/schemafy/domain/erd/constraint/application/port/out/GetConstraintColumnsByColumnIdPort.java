package com.schemafy.domain.erd.constraint.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnsByColumnIdPort {

  Mono<List<ConstraintColumn>> findConstraintColumnsByColumnId(String columnId);

}
