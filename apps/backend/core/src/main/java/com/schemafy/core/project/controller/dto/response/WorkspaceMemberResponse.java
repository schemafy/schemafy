package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.service.dto.WorkspaceMemberDetail;
import com.schemafy.core.user.repository.entity.User;

public record WorkspaceMemberResponse(String workspaceId, String userId,
    String userName, String userEmail, String role, Instant joinedAt) {

  public static WorkspaceMemberResponse of(WorkspaceMember member,
      User user) {
    return new WorkspaceMemberResponse(
        member.getWorkspaceId(),
        user.getId(),
        user.getName(),
        user.getEmail(),
        member.getRole(),
        member.getCreatedAt());
  }

  public static WorkspaceMemberResponse from(WorkspaceMemberDetail detail) {
    return of(detail.member(), detail.user());
  }

}
