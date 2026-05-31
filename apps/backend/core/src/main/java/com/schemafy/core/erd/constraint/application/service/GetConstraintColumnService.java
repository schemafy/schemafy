package com.schemafy.core.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnUseCase;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.CONSTRAINT_COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = CONSTRAINT_COLUMN, id = "constraintColumnId"))
public class GetConstraintColumnService implements GetConstraintColumnUseCase {

  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;

  @Override
  public Mono<ConstraintColumn> getConstraintColumn(GetConstraintColumnQuery query) {
    return getConstraintColumnByIdPort.findConstraintColumnById(query.constraintColumnId())
        .switchIfEmpty(Mono.error(
            new DomainException(ConstraintErrorCode.COLUMN_NOT_FOUND,
                "Constraint column not found: " + query.constraintColumnId())));
  }

}
