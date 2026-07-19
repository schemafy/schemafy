package com.schemafy.core.erd.schema.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.schema.domain.Schema;

@Component
class SchemaMapper {

  SchemaEntity toEntity(Schema schema) {
    return new SchemaEntity(
        schema.id(),
        schema.projectId(),
        schema.name(),
        schema.charset(),
        schema.collation());
  }

  Schema toDomain(SchemaEntity entity) {
    return new Schema(
        entity.getId(),
        entity.getProjectId(),
        entity.getName(),
        entity.getCharset(),
        entity.getCollation());
  }

}
