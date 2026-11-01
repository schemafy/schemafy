package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.api.common.constant.UserDisplayConstants;
import com.schemafy.api.project.orchestrator.dto.WorkspaceMemberView;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;

public record WorkspaceMemberResponse(String workspaceId, String userId,
    String userName, String userEmail, String role, Instant joinedAt) {

  public static WorkspaceMemberResponse of(WorkspaceMember member,
      User user) {
    boolean active = user.status() == UserStatus.ACTIVE;
    return new WorkspaceMemberResponse(
        member.getWorkspaceId(),
        user.id(),
        active ? user.name() : UserDisplayConstants.WITHDRAWN_USER_LABEL,
        active ? user.email() : "",
        member.getRole(),
        member.getCreatedAt());
  }

  public static WorkspaceMemberResponse from(WorkspaceMemberView detail) {
    return of(detail.member(), detail.user());
  }

}
