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
@Table("user_auth_providers")
class UserAuthProviderEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("user_id")
  private String userId;

  @Column("provider")
  private String provider;

  @Column("provider_user_id")
  private String providerUserId;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  UserAuthProviderEntity(
      String id,
      String userId,
      String provider,
      String providerUserId,
      Instant createdAt,
      Instant updatedAt,
      Instant deletedAt) {
    this.id = id;
    this.userId = userId;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  @Override
  public String getId() { return id; }

  @Override
  public boolean isNew() { return this.createdAt == null; }

}
