package com.schemafy.core.project.application.access;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ColumnProjectAccessResourceResolver implements ProjectAccessResourceResolver {

  private final GetProjectIdByAccessResourcePort projectIdPort;

  @Override
  public Set<ProjectAccessResourceType> resourceTypes() {
    return Set.of(ProjectAccessResourceType.COLUMN);
  }

  @Override
  public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
    return projectIdPort.findProjectId(type, id)
        .switchIfEmpty(Mono.error(
            new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

}
