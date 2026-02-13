package com.schemafy.domain.erd.vendor.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_vendors")
class DbVendorEntity {

  @Id
  @Column("display_name")
  private String displayName;

  @Column("name")
  private String name;

  @Column("version")
  private String version;

  @Column("datatype_mappings")
  private String datatypeMappings;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

}
