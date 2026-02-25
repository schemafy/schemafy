package com.schemafy.domain.erd.index.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
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
@Table("db_indexes")
public class IndexEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("table_id")
  private String tableId;

  @Column("name")
  private String name;

  @Column("type")
  private String type;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Version
  private Long version;

  IndexEntity(
      String id,
      String tableId,
      String name,
      String type) {
    this.id = id;
    this.tableId = tableId;
    this.name = name;
    this.type = type;
  }

  @Override
  public boolean isNew() { return this.version == null; }

  @Override
  public String getId() { return this.id; }

  public String getTableId() { return tableId; }

  public String getName() { return name; }

  public String getType() { return type; }

  public void setName(String name) { this.name = name; }

  public void setType(String type) { this.type = type; }

}
