package com.schemafy.core.project.domain;

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

  private String projectId;
  private Boolean isActive;

  public static ShareLink create(String id, String projectId) {
    if (projectId == null || projectId.isBlank()) {
      throw new DomainException(ShareLinkErrorCode.INVALID_PROJECT_ID);
    }
    ShareLink shareLink = new ShareLink(projectId, true);
    shareLink.setId(id);
    return shareLink;
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public boolean isActive() { return Boolean.TRUE.equals(isActive) && !isDeleted(); }

}
