package com.schemafy.domain.erd.schema.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.schema.domain.Schema;

@Component
class SchemaMapper {

  SchemaEntity toEntity(Schema schema) {
    return SchemaEntity.builder()
        .id(schema.id())
        .projectId(schema.projectId())
        .dbVendorName(schema.dbVendorName())
        .name(schema.name())
        .charset(schema.charset())
        .collation(schema.collation())
        .build();
  }

  Schema toDomain(SchemaEntity entity) {
    return new Schema(
        entity.getId(),
        entity.getProjectId(),
        entity.getDbVendorName(),
        entity.getName(),
        entity.getCharset(),
        entity.getCollation());
  }

}
