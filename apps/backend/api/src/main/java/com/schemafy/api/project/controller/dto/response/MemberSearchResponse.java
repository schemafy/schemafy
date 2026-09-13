package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.application.port.in.MemberSearchResult;

public record MemberSearchResponse(
    String userId,
    String userName,
    String userEmail,
    String role,
    Instant joinedAt) {

  public static MemberSearchResponse from(MemberSearchResult result) {
    return new MemberSearchResponse(
        result.userId(),
        result.userName(),
        result.userEmail(),
        result.role(),
        result.joinedAt());
  }

}
