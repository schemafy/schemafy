package com.schemafy.core.project.domain;

import java.util.regex.Pattern;

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

  private static final Pattern CODE_PATTERN = Pattern.compile("[0-9a-f]{32}");

  private String projectId;
  private String code;
  private Boolean isActive;

  public static boolean isValidCode(String code) {
    return code != null && CODE_PATTERN.matcher(code).matches();
  }

  public static ShareLink create(String id, String projectId, String code) {
    if (projectId == null || projectId.isBlank()) {
      throw new DomainException(ShareLinkErrorCode.INVALID_PROJECT_ID);
    }
    if (!isValidCode(code)) {
      throw new DomainException(ShareLinkErrorCode.INVALID_LINK);
    }
    ShareLink shareLink = new ShareLink(projectId, code, true);
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
