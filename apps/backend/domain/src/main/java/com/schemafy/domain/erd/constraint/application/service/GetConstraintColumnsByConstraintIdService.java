package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetConstraintColumnsByConstraintIdService
    implements GetConstraintColumnsByConstraintIdUseCase {

  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Override
  public Mono<List<ConstraintColumn>> getConstraintColumnsByConstraintId(
      GetConstraintColumnsByConstraintIdQuery query) {
    return getConstraintColumnsByConstraintIdPort
        .findConstraintColumnsByConstraintId(query.constraintId());
  }

}
