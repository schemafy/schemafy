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
  public String getId() { return this.id; }

}
