package com.schemafy.core.project.application.access;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ConstraintProjectAccessResourceResolver implements ProjectAccessResourceResolver {

  private final GetProjectIdByAccessResourcePort projectIdPort;

  @Override
  public Set<ProjectAccessResourceType> resourceTypes() {
    return Set.of(
        ProjectAccessResourceType.CONSTRAINT,
        ProjectAccessResourceType.CONSTRAINT_COLUMN);
  }

  @Override
  public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
    return switch (type) {
    case CONSTRAINT -> resolveConstraintParent(id);
    case CONSTRAINT_COLUMN -> resolveConstraintColumnParent(id);
    default -> Mono.error(new IllegalStateException("Unsupported constraint access type: " + type));
    };
  }

  private Mono<ProjectAccessResourceRef> resolveConstraintParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.CONSTRAINT, id)
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND, "Constraint not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

  private Mono<ProjectAccessResourceRef> resolveConstraintColumnParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.CONSTRAINT_COLUMN, id)
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.COLUMN_NOT_FOUND,
            "Constraint column not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

}
