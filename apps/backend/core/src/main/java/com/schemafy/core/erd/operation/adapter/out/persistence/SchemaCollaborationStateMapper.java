package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;

@Component
class SchemaCollaborationStateMapper {

  SchemaCollaborationStateEntity toEntity(SchemaCollaborationState schemaCollaborationState) {
    return new SchemaCollaborationStateEntity(
        schemaCollaborationState.schemaId(),
        schemaCollaborationState.projectId(),
        schemaCollaborationState.currentRevision(),
        schemaCollaborationState.createdAt(),
        schemaCollaborationState.updatedAt());
  }

  SchemaCollaborationState toDomain(SchemaCollaborationStateEntity entity) {
    return new SchemaCollaborationState(
        entity.getSchemaId(),
        entity.getProjectId(),
        entity.getCurrentRevision(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

}
