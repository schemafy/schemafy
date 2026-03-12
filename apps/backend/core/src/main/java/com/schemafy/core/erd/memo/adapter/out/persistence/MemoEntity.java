package com.schemafy.core.erd.memo.adapter.out.persistence;

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
@Table("memos")
class MemoEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("schema_id")
  private String schemaId;

  @Column("author_id")
  private String authorId;

  @Column("positions")
  private String positions;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  MemoEntity(String id, String schemaId, String authorId, String positions,
      Instant createdAt, Instant updatedAt, Instant deletedAt) {
    this.id = id;
    this.schemaId = schemaId;
    this.authorId = authorId;
    this.positions = positions;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  @Override
  public String getId() { return id; }

  @Override
  public boolean isNew() { return this.createdAt == null; }

}
