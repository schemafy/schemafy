package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_tables")
public class TableEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("schema_id")
  private String schemaId;

  @Column("name")
  private String name;

  @Column("charset")
  private String charset;

  @Column("collation")
  private String collation;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  @Builder
  private TableEntity(String id, String schemaId, String name,
      String charset, String collation) {
    this.id = id;
    this.schemaId = schemaId;
    this.name = name;
    this.charset = charset;
    this.collation = collation;
  }

  @Override
  public boolean isNew() { return this.createdAt == null; }

}
