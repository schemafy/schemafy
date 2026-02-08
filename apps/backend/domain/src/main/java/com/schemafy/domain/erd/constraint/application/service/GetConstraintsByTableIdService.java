package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetConstraintsByTableIdService implements GetConstraintsByTableIdUseCase {

  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Override
  public Mono<List<Constraint>> getConstraintsByTableId(GetConstraintsByTableIdQuery query) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(query.tableId());
  }

}
