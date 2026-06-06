package com.schemafy.core.erd.constraint.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = TABLE, id = "tableId"))
public class GetConstraintsByTableIdService implements GetConstraintsByTableIdUseCase {

  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Override
  public Mono<List<Constraint>> getConstraintsByTableId(GetConstraintsByTableIdQuery query) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(query.tableId());
  }

}
