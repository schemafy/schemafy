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
}
