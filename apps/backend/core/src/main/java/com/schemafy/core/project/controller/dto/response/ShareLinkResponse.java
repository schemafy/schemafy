package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.ShareLink;

public record ShareLinkResponse(
    String id,
    String projectId,
    String url,
    Instant expiresAt,
    Boolean isRevoked,
    Instant lastAccessedAt,
    Long accessCount,
    Instant createdAt) {

  public static ShareLinkResponse of(ShareLink shareLink, String baseUrl) {
    return new ShareLinkResponse(
        shareLink.getId(),
        shareLink.getProjectId(),
        baseUrl + "/share/" + shareLink.getCode(),
        shareLink.getExpiresAt(),
        shareLink.getIsRevoked(),
        shareLink.getLastAccessedAt(),
        shareLink.getAccessCount(),
        shareLink.getCreatedAt());
  }

}
