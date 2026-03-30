package com.schemafy.core.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdUseCase;
import com.schemafy.core.erd.schema.application.port.out.ActiveProjectExistsPort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemasByProjectIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class GetSchemasByProjectIdService implements GetSchemasByProjectIdUseCase {

  private final ActiveProjectExistsPort activeProjectExistsPort;
  private final GetSchemasByProjectIdPort getSchemasByProjectIdPort;

  @Override
  public Flux<Schema> getSchemasByProjectId(GetSchemasByProjectIdQuery query) {
    return activeProjectExistsPort.existsActiveProjectById(query.projectId())
        .flatMapMany(projectExists -> {
          if (!projectExists) {
            return Flux.error(new DomainException(ProjectErrorCode.NOT_FOUND,
                "Project not found: " + query.projectId()));
          }

          return getSchemasByProjectIdPort.findSchemasByProjectId(query.projectId());
        });
  }

}
