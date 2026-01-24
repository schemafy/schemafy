package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("db_relationships")
public class RelationshipEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("pk_table_id")
  private String pkTableId;

  @Column("fk_table_id")
  private String fkTableId;

  @Column("name")
  private String name;

  @Column("kind")
  private String kind;

  @Column("cardinality")
  private String cardinality;

  @Column("extra")
  private String extra;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  protected RelationshipEntity() {}

  RelationshipEntity(
      String id,
      String pkTableId,
      String fkTableId,
      String name,
      String kind,
      String cardinality,
      String extra) {
    this.id = id;
    this.pkTableId = pkTableId;
    this.fkTableId = fkTableId;
    this.name = name;
    this.kind = kind;
    this.cardinality = cardinality;
    this.extra = extra;
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

  public String getPkTableId() {
    return pkTableId;
  }

  public void setPkTableId(String pkTableId) {
    this.pkTableId = pkTableId;
  }

  public String getFkTableId() {
    return fkTableId;
  }

  public void setFkTableId(String fkTableId) {
    this.fkTableId = fkTableId;
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

  public String getCardinality() {
    return cardinality;
  }

  public void setCardinality(String cardinality) {
    this.cardinality = cardinality;
  }

  public String getExtra() {
    return extra;
  }

  public void setExtra(String extra) {
    this.extra = extra;
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
