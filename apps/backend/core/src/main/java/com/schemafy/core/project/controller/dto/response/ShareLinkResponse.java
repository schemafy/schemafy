package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.domain.project.domain.ShareLink;

public record ShareLinkResponse(
    String id,
    String projectId,
    String url,
    Instant expiresAt,
    Boolean isRevoked,
    Instant lastAccessedAt,
    Long accessCount,
    Instant createdAt) {

  public static ShareLinkResponse of(ShareLink shareLink, String baseUrl, String version) {
    return new ShareLinkResponse(
        shareLink.getId(),
        shareLink.getProjectId(),
        buildPublicShareUrl(baseUrl, version, shareLink.getCode()),
        shareLink.getExpiresAt(),
        shareLink.getIsRevoked(),
        shareLink.getLastAccessedAt(),
        shareLink.getAccessCount(),
        shareLink.getCreatedAt());
  }

  private static String buildPublicShareUrl(String baseUrl, String version, String code) {
    String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String publicApiPath = ApiPath.PUBLIC_API.replace("{version}", version);
    return normalizedBaseUrl + publicApiPath + "/share/" + code;
  }

}
