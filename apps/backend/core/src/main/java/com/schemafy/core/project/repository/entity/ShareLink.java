package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.project.exception.ShareLinkErrorCode;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.domain.common.exception.DomainException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("share_links")
public class ShareLink extends BaseEntity {

  private String projectId;

  private byte[] tokenHash;

  private String role;

  private Instant expiresAt;

  private Boolean isRevoked;

  private Instant lastAccessedAt;

  private Long accessCount;

  public static ShareLink create(String projectId, byte[] tokenHash,
      ShareLinkRole role, Instant expiresAt) {
    if (projectId == null || projectId.isBlank()) {
      throw new DomainException(
          ShareLinkErrorCode.INVALID_PROJECT_ID);
    }
    if (tokenHash == null || tokenHash.length != 32) {
      throw new DomainException(
          ShareLinkErrorCode.INVALID_TOKEN_HASH);
    }
    if (role == null) {
      throw new DomainException(ShareLinkErrorCode.INVALID_ROLE);
    }
    if (expiresAt != null && !Instant.now().isBefore(expiresAt)) {
      throw new DomainException(
          ShareLinkErrorCode.INVALID_EXPIRATION);
    }

    ShareLink shareLink = new ShareLink(
        projectId,
        tokenHash,
        role.getValue(),
        expiresAt,
        false,
        null,
        0L);
    shareLink.setId(UlidGenerator.generate());
    return shareLink;
  }

  public ShareLinkRole getRoleAsEnum() { return ShareLinkRole.fromString(this.role); }

  public void revoke() {
    this.isRevoked = true;
  }

  public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }

  public boolean isActive() { return !isRevoked && !isExpired() && !isDeleted(); }

}
