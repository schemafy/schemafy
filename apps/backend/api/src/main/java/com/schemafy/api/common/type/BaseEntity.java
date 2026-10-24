package com.schemafy.api.common.type;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity implements Persistable<String> {

  @Id
  @Setter(AccessLevel.PROTECTED)
  protected String id;

  @CreatedDate
  protected Instant createdAt;

  @LastModifiedDate
  protected Instant updatedAt;

  protected Instant deletedAt;

  @Override
  public String getId() { return id; }

  @Override
  @JsonIgnore
  public boolean isNew() { return this.createdAt == null; }

  public void delete() {
    this.deletedAt = Instant.now();
  }

  public void restore() {
    this.deletedAt = null;
  }

  @JsonIgnore
  public boolean isDeleted() { return deletedAt != null; }

}
