package com.schemafy.domain.erd.relationship.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

@Component
class RelationshipColumnMapper {

  RelationshipColumnEntity toEntity(RelationshipColumn relationshipColumn) {
    return new RelationshipColumnEntity(
        relationshipColumn.id(),
        relationshipColumn.relationshipId(),
        relationshipColumn.pkColumnId(),
        relationshipColumn.fkColumnId(),
        relationshipColumn.seqNo());
  }

  RelationshipColumn toDomain(RelationshipColumnEntity entity) {
    return new RelationshipColumn(
        entity.getId(),
        entity.getRelationshipId(),
        entity.getPkColumnId(),
        entity.getFkColumnId(),
        entity.getSeqNo());
  }

}
