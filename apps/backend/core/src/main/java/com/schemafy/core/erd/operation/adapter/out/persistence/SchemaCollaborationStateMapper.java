package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;

@Component
class SchemaCollaborationStateMapper {

  SchemaCollaborationStateEntity toEntity(SchemaCollaborationState schemaCollaborationState) {
    return SchemaCollaborationStateEntity.builder()
        .schemaId(schemaCollaborationState.schemaId())
        .projectId(schemaCollaborationState.projectId())
        .currentRevision(schemaCollaborationState.currentRevision())
        .createdAt(schemaCollaborationState.createdAt())
        .updatedAt(schemaCollaborationState.updatedAt())
        .build();
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
