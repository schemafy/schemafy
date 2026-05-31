package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.project.application.access.GetProjectIdByAccessResourcePort;
import com.schemafy.core.project.application.access.ProjectAccessResourceType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class ErdProjectIdByAccessResourceAdapter implements GetProjectIdByAccessResourcePort {

  private final ErdProjectIdByAccessResourceRepository repository;

  @Override
  public Mono<String> findProjectId(ProjectAccessResourceType type, String id) {
    return switch (type) {
    case NONE -> Mono.error(new IllegalStateException("Project access resource type is missing"));
    case PROJECT -> repository.findProjectIdByProjectId(id);
    case SCHEMA -> repository.findProjectIdBySchemaId(id);
    case TABLE -> repository.findProjectIdByTableId(id);
    case COLUMN -> repository.findProjectIdByColumnId(id);
    case CONSTRAINT -> repository.findProjectIdByConstraintId(id);
    case CONSTRAINT_COLUMN -> repository
        .findProjectIdByConstraintColumnId(id);
    case INDEX -> repository.findProjectIdByIndexId(id);
    case INDEX_COLUMN -> repository.findProjectIdByIndexColumnId(id);
    case RELATIONSHIP -> repository.findProjectIdByRelationshipId(id);
    case RELATIONSHIP_COLUMN -> repository
        .findProjectIdByRelationshipColumnId(id);
    case MEMO -> repository.findProjectIdByMemoId(id);
    case MEMO_COMMENT -> repository.findProjectIdByMemoCommentId(id);
    case OPERATION -> repository.findProjectIdByOperationId(id);
    };
  }

}
