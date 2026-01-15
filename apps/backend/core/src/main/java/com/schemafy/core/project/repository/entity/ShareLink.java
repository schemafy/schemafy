package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

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

  private String code;

  private Instant expiresAt;

  private Boolean isRevoked;

  private Instant lastAccessedAt;

  private Long accessCount;

  public static ShareLink create(String projectId, String code, Instant expiresAt) {
    if (projectId == null || projectId.isBlank()) {
      throw new BusinessException(
          ErrorCode.SHARE_LINK_INVALID_PROJECT_ID);
    }
    if (code == null || code.isBlank() || code.length() < 22) {
      throw new BusinessException(
          ErrorCode.SHARE_LINK_INVALID_CODE);
    }
    if (expiresAt != null && !Instant.now().isBefore(expiresAt)) {
      throw new BusinessException(
          ErrorCode.SHARE_LINK_INVALID_EXPIRATION);
    }

    ShareLink shareLink = new ShareLink(
        projectId,
        code,
        expiresAt,
        false,
        null,
        0L);
    shareLink.setId(UlidGenerator.generate());
    return shareLink;
  }

  public void revoke() {
    this.isRevoked = true;
  }

  public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }

  public boolean isActive() { return !isRevoked && !isExpired() && !isDeleted(); }

}
