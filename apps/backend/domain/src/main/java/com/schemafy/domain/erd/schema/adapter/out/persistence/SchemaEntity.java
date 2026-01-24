package com.schemafy.domain.erd.schema.adapter.out.persistence;

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
@Table("db_schemas")
public class SchemaEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("project_id")
  private String projectId;

  @Column("db_vendor_name")
  private String dbVendorName;

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
  private SchemaEntity(String id, String projectId, String dbVendorName,
      String name, String charset, String collation) {
    this.id = id;
    this.projectId = projectId;
    this.dbVendorName = dbVendorName;
    this.name = name;
    this.charset = charset;
    this.collation = collation;
  }

  @Override
  public boolean isNew() { return this.createdAt == null; }

}
