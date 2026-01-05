package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_relationship_columns")
public class RelationshipColumn extends BaseEntity {

  @Column("relationship_id")
  private String relationshipId;

  @Column("fk_column_id")
  private String fkColumnId;

  @Column("pk_column_id")
  private String pkColumnId;

  @Column("seq_no")
  private int seqNo;

  @Builder(builderMethodName = "builder", buildMethodName = "build")
  private static RelationshipColumn newRelationshipColumn(
      String relationshipId,
      String fkColumnId, String pkColumnId, int seqNo) {
    RelationshipColumn relationshipColumn = new RelationshipColumn(
        relationshipId,
        fkColumnId, pkColumnId, seqNo);
    relationshipColumn.setId(UlidGenerator.generate());
    return relationshipColumn;
  }

}
