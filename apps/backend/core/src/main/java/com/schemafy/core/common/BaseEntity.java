package com.schemafy.core.common;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity implements Persistable<String> {

  @Id
  protected String id;

  @CreatedDate
  protected Instant createdAt;

  @LastModifiedDate
  protected Instant updatedAt;

  protected Instant deletedAt;

  @Override
  public String getId() { return id; }

  @Override
  public boolean isNew() { return this.createdAt == null; }

  protected void setId(String id) { this.id = id; }

  public void delete() {
    this.deletedAt = Instant.now();
  }

  public void restore() {
    this.deletedAt = null;
  }

  public boolean isDeleted() { return deletedAt != null; }

}
