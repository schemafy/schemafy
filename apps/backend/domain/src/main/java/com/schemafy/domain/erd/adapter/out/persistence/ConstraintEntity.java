package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("db_constraints")
public class ConstraintEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("table_id")
  private String tableId;

  @Column("name")
  private String name;

  @Column("kind")
  private String kind;

  @Column("check_expr")
  private String checkExpr;

  @Column("default_expr")
  private String defaultExpr;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  protected ConstraintEntity() {}

  ConstraintEntity(
      String id,
      String tableId,
      String name,
      String kind,
      String checkExpr,
      String defaultExpr) {
    this.id = id;
    this.tableId = tableId;
    this.name = name;
    this.kind = kind;
    this.checkExpr = checkExpr;
    this.defaultExpr = defaultExpr;
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

  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getCheckExpr() {
    return checkExpr;
  }

  public void setCheckExpr(String checkExpr) {
    this.checkExpr = checkExpr;
  }

  public String getDefaultExpr() {
    return defaultExpr;
  }

  public void setDefaultExpr(String defaultExpr) {
    this.defaultExpr = defaultExpr;
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
