package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.domain.Relationship;
import com.schemafy.domain.erd.domain.type.Cardinality;
import com.schemafy.domain.erd.domain.type.RelationshipKind;

@Component
class RelationshipMapper {

  RelationshipEntity toEntity(Relationship relationship) {
    return new RelationshipEntity(
        relationship.id(),
        relationship.pkTableId(),
        relationship.fkTableId(),
        relationship.name(),
        relationship.kind().name(),
        relationship.cardinality().name(),
        relationship.extra());
  }

  Relationship toDomain(RelationshipEntity entity) {
    return new Relationship(
        entity.getId(),
        entity.getPkTableId(),
        entity.getFkTableId(),
        entity.getName(),
        RelationshipKind.valueOf(entity.getKind()),
        Cardinality.valueOf(entity.getCardinality()),
        entity.getExtra());
  }
}
