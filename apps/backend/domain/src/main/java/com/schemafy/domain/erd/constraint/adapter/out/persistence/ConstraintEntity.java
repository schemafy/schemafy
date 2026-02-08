package com.schemafy.domain.erd.constraint.adapter.out.persistence;

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
  public String getId() { return this.id; }

}
