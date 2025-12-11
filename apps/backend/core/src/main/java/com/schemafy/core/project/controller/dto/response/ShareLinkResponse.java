package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schemafy.core.project.repository.entity.ShareLink;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShareLinkResponse(

        String id,

        String projectId,

        String token,

        String role,

        Instant expiresAt,

        Boolean isRevoked,

        Instant lastAccessedAt,

        Long accessCount,

        Instant createdAt) {

    public static ShareLinkResponse of(ShareLink shareLink, String token) {
        return new ShareLinkResponse(shareLink.getId(),
                shareLink.getProjectId(),
                token, shareLink.getRole(), shareLink.getExpiresAt(),
                shareLink.getIsRevoked(), shareLink.getLastAccessedAt(),
                shareLink.getAccessCount(), shareLink.getCreatedAt());
    }

    public static ShareLinkResponse from(ShareLink shareLink) {
        return new ShareLinkResponse(shareLink.getId(),
                shareLink.getProjectId(),
                null, shareLink.getRole(), shareLink.getExpiresAt(),
                shareLink.getIsRevoked(), shareLink.getLastAccessedAt(),
                shareLink.getAccessCount(), shareLink.getCreatedAt());
    }

}
