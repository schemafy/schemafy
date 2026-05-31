package com.schemafy.core.project.application.access;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class IndexProjectAccessResourceResolver implements ProjectAccessResourceResolver {

  private final GetProjectIdByAccessResourcePort projectIdPort;

  @Override
  public Set<ProjectAccessResourceType> resourceTypes() {
    return Set.of(ProjectAccessResourceType.INDEX, ProjectAccessResourceType.INDEX_COLUMN);
  }

  @Override
  public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
    return switch (type) {
    case INDEX -> resolveIndexParent(id);
    case INDEX_COLUMN -> resolveIndexColumnParent(id);
    default -> Mono.error(new IllegalStateException("Unsupported index access type: " + type));
    };
  }

  private Mono<ProjectAccessResourceRef> resolveIndexParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.INDEX, id)
        .switchIfEmpty(Mono.error(
            new DomainException(IndexErrorCode.NOT_FOUND, "Index not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

  private Mono<ProjectAccessResourceRef> resolveIndexColumnParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.INDEX_COLUMN, id)
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

}
