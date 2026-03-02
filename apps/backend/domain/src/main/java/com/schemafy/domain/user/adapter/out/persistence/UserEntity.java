package com.schemafy.domain.user.adapter.out.persistence;

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
@Table("users")
class UserEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("email")
  private String email;

  @Column("name")
  private String name;

  @Column("password")
  private String password;

  @Column("status")
  private String status;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  UserEntity(
      String id,
      String email,
      String name,
      String password,
      String status,
      Instant createdAt,
      Instant updatedAt,
      Instant deletedAt) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.password = password;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  @Override
  public String getId() { return id; }

  @Override
  public boolean isNew() { return this.createdAt == null; }

}

