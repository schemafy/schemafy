package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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

  protected RelationshipColumnEntity() {}

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

  public void setId(String id) {
    this.id = id;
  }

  public String getRelationshipId() {
    return relationshipId;
  }

  public void setRelationshipId(String relationshipId) {
    this.relationshipId = relationshipId;
  }

  public String getPkColumnId() {
    return pkColumnId;
  }

  public void setPkColumnId(String pkColumnId) {
    this.pkColumnId = pkColumnId;
  }

  public String getFkColumnId() {
    return fkColumnId;
  }

  public void setFkColumnId(String fkColumnId) {
    this.fkColumnId = fkColumnId;
  }

  public int getSeqNo() {
    return seqNo;
  }

  public void setSeqNo(int seqNo) {
    this.seqNo = seqNo;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }
}
