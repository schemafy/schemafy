package com.schemafy.core.project.application.access;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class RelationshipProjectAccessResourceResolver implements ProjectAccessResourceResolver {

  private final GetProjectIdByAccessResourcePort projectIdPort;

  @Override
  public Set<ProjectAccessResourceType> resourceTypes() {
    return Set.of(
        ProjectAccessResourceType.RELATIONSHIP,
        ProjectAccessResourceType.RELATIONSHIP_COLUMN);
  }

  @Override
  public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
    return switch (type) {
    case RELATIONSHIP -> resolveRelationshipParent(id);
    case RELATIONSHIP_COLUMN -> resolveRelationshipColumnParent(id);
    default -> Mono.error(new IllegalStateException("Unsupported relationship access type: " + type));
    };
  }

  private Mono<ProjectAccessResourceRef> resolveRelationshipParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.RELATIONSHIP, id)
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND, "Relationship not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

  private Mono<ProjectAccessResourceRef> resolveRelationshipColumnParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.RELATIONSHIP_COLUMN, id)
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.COLUMN_NOT_FOUND,
            "Relationship column not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

}
