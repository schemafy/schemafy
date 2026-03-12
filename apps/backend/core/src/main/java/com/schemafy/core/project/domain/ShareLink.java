package com.schemafy.core.project.domain;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.BaseEntity;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("share_links")
public class ShareLink extends BaseEntity {

  private static final int DEFAULT_DURATION_DAYS = 14;

  private String projectId;
  private String code;
  private Instant expiresAt;
  private Boolean isRevoked;
  private Instant lastAccessedAt;
  private Long accessCount;

  public static ShareLink create(String id, String projectId, String code) {
    Instant expiresAt = Instant.now()
        .plus(Duration.ofDays(DEFAULT_DURATION_DAYS));
    return create(id, projectId, code, expiresAt);
  }

  public static ShareLink create(
      String id,
      String projectId,
      String code,
      Instant expiresAt) {
    if (projectId == null || projectId.isBlank()) {
      throw new DomainException(ShareLinkErrorCode.INVALID_PROJECT_ID);
    }
    if (code == null || code.isBlank()) {
      throw new DomainException(ShareLinkErrorCode.INVALID_LINK);
    }

    ShareLink shareLink = new ShareLink(projectId, code, expiresAt, false,
        null, 0L);
    shareLink.setId(id);
    return shareLink;
  }

  public void revoke() {
    this.isRevoked = true;
  }

  public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }

  public boolean isActive() { return !Boolean.TRUE.equals(isRevoked) && !isExpired() && !isDeleted(); }

}
