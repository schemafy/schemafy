package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_relationship_columns")
public class RelationshipColumnEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("relationship_id")
  private String relationshipId;

  @Column("pk_column_id")
  private String pkColumnId;

  @Column("fk_column_id")
  private String fkColumnId;

  @Column("seq_no")
  private int seqNo;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  RelationshipColumnEntity(
      String id,
      String relationshipId,
      String pkColumnId,
      String fkColumnId,
      int seqNo) {
    this.id = id;
    this.relationshipId = relationshipId;
    this.pkColumnId = pkColumnId;
    this.fkColumnId = fkColumnId;
    this.seqNo = seqNo;
  }

  @Override
  public boolean isNew() {
    return this.createdAt == null;
  }

  @Override
  public String getId() {
    return this.id;
  }
}
