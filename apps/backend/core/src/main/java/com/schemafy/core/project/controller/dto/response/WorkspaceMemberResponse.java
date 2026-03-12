package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.orchestrator.dto.WorkspaceMemberView;
import com.schemafy.domain.project.domain.WorkspaceMember;
import com.schemafy.domain.user.domain.User;

public record WorkspaceMemberResponse(String workspaceId, String userId,
    String userName, String userEmail, String role, Instant joinedAt) {

  public static WorkspaceMemberResponse of(WorkspaceMember member,
      User user) {
    return new WorkspaceMemberResponse(
        member.getWorkspaceId(),
        user.id(),
        user.name(),
        user.email(),
        member.getRole(),
        member.getCreatedAt());
  }

  public static WorkspaceMemberResponse from(WorkspaceMemberView detail) {
    return of(detail.member(), detail.user());
  }

}
