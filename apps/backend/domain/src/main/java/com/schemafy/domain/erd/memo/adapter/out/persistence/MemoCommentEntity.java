package com.schemafy.domain.erd.memo.adapter.out.persistence;

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
@Table("memo_comments")
class MemoCommentEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("memo_id")
  private String memoId;

  @Column("author_id")
  private String authorId;

  @Column("body")
  private String body;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  MemoCommentEntity(String id, String memoId, String authorId, String body,
      Instant createdAt, Instant updatedAt, Instant deletedAt) {
    this.id = id;
    this.memoId = memoId;
    this.authorId = authorId;
    this.body = body;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  @Override
  public String getId() { return id; }

  @Override
  public boolean isNew() { return this.createdAt == null; }

}
