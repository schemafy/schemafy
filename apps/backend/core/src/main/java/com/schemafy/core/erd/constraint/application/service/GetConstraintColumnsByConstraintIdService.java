package com.schemafy.core.erd.constraint.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.CONSTRAINT;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = CONSTRAINT, id = "constraintId"))
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
