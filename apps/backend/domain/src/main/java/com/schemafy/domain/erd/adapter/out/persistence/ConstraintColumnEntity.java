package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("db_constraint_columns")
public class ConstraintColumnEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("constraint_id")
  private String constraintId;

  @Column("column_id")
  private String columnId;

  @Column("seq_no")
  private int seqNo;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  protected ConstraintColumnEntity() {}

  ConstraintColumnEntity(
      String id,
      String constraintId,
      String columnId,
      int seqNo) {
    this.id = id;
    this.constraintId = constraintId;
    this.columnId = columnId;
    this.seqNo = seqNo;
  }

  @Override
  public boolean isNew() { return this.createdAt == null; }

  @Override
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getConstraintId() {
    return constraintId;
  }

  public void setConstraintId(String constraintId) {
    this.constraintId = constraintId;
  }

  public String getColumnId() {
    return columnId;
  }

  public void setColumnId(String columnId) {
    this.columnId = columnId;
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
