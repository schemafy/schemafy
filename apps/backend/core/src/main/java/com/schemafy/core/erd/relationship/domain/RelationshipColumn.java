package com.schemafy.core.erd.relationship.domain;

public record RelationshipColumn(
    String id,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {

  public RelationshipColumn withSeqNo(int nextSeqNo) {
    return new RelationshipColumn(
        id,
        relationshipId,
        pkColumnId,
        fkColumnId,
        nextSeqNo);
  }

}
