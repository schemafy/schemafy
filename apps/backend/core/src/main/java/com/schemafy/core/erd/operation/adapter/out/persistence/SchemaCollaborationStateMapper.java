package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;

@Component
class SchemaCollaborationStateMapper {

  SchemaCollaborationStateEntity toEntity(SchemaCollaborationState schemaCollaborationState) {
    var entity = new SchemaCollaborationStateEntity();
    entity.setSchemaId(schemaCollaborationState.schemaId());
    entity.setProjectId(schemaCollaborationState.projectId());
    entity.setCurrentRevision(schemaCollaborationState.currentRevision());
    entity.setCreatedAt(schemaCollaborationState.createdAt());
    entity.setUpdatedAt(schemaCollaborationState.updatedAt());
    return entity;
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
