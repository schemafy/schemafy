package com.schemafy.core.project.application.access;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class TableProjectAccessResourceResolver implements ProjectAccessResourceResolver {

  private final GetProjectIdByAccessResourcePort projectIdPort;

  @Override
  public Set<ProjectAccessResourceType> resourceTypes() {
    return Set.of(ProjectAccessResourceType.TABLE);
  }

  @Override
  public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
    return projectIdPort.findProjectId(type, id)
        .switchIfEmpty(Mono.error(
            new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

}
